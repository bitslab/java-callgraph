package gr.gousiosg.javacg.stat.support;

import gr.gousiosg.javacg.dyn.Pair;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.yaml.snakeyaml.Yaml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class RepoTool {

    private static final Logger LOGGER = LoggerFactory.getLogger(RepoTool.class);
    final private String name;
    final private String URL;
    final private String checkoutID;
    final private String patchName;
    final private String subProject;
    final private String mvnOptions;
    private List<Map<String, String>> properties;
    private Git git;
    final private String timeStamp;

    final private String mainJar;

    final private String testJar;


    private RepoTool(String name, String URL, String checkoutID, String patchName, String subProject, String mvnOptions, String mainJar, String testJar){
        this.name = name;
        this.URL = URL;
        this.checkoutID = checkoutID;
        this.patchName = patchName;
        this.subProject = subProject;
        this.mvnOptions = mvnOptions;
        this.mainJar = mainJar;
        this.testJar = testJar;

        this.timeStamp = String.valueOf(java.time.LocalDateTime.now()).replace(':', '_');
    }

    public RepoTool(String name) throws FileNotFoundException {
        this(name, String.valueOf(java.time.LocalDateTime.now()).replace(':', '_'));
    }

    public RepoTool(String name, String timeStamp) throws FileNotFoundException {
        // @todo Perhaps using objects to store configuration data so we don't have to have unchecked casts e.g. https://www.baeldung.com/java-snake-yaml

        Yaml yaml = new Yaml();
        InputStream inputStream = new FileInputStream("artifacts/configs/" + name + "/" + name + ".yaml");
        Map<String, Object> data = yaml.load(inputStream);

        this.name = name;
        URL = (String) data.get("URL");
        checkoutID = (String) data.get("checkoutID");
        patchName = (String) data.get("patchName");
        subProject = (String) data.getOrDefault("subProject", "");
        mvnOptions = (String) data.getOrDefault("mvnOptions", "");
        properties = (List<Map<String,String>>) data.get("properties");
        mainJar = (String) data.getOrDefault("mainJar", new ArrayList<>());
        testJar = (String) data.getOrDefault("testJar", Optional.empty());

        this.timeStamp = timeStamp;
    }

    public void cloneRepo() throws GitAPIException, JGitInternalException {
        this.git = Git.cloneRepository()
                .setDirectory(new File(name))
                .setURI(URL)
                .call();
        this.git.checkout()
                .setName(checkoutID)
                .call();
    }

    public void applyPatch() throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder();
        if(isWindows())
            pb.command("cmd.exe", "/c", "git", "apply", patchName, "--directory", name);
        else
            pb.command("bash", "-c", "patch -p1 -d " + name + " < " + patchName);
        Process process = pb.start();
        process.waitFor();
    }

    public void buildJars() throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder();
        if(isWindows())
            pb.command("cmd.exe", "/c", "mvn", "install", "-DskipTests");
        else
            pb.command("bash", "-c", "mvn install -DskipTests");
        pb.directory(new File(this.name));
        Process process = pb.start();
        BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while((line = br.readLine()) != null)
            LOGGER.info(line);
        process.waitFor();
        moveJars();
    }

    public void testProperty(String property) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder();
        if(isWindows())
            pb.command("cmd.exe", "/c", "mvn", "test", mvnOptions, "-Dtest=" + property);
        else
            pb.command("bash", "-c", "mvn test " + mvnOptions + " -Dtest=" + property);
        pb.directory(new File(this.name));
        long start = System.nanoTime();
        Process process = pb.start();
        BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while((line = br.readLine()) != null)
            LOGGER.info(line);
        process.waitFor();
        long end = System.nanoTime();
        moveJacoco(property, end - start);
    }

    public void cleanTarget() throws IOException, InterruptedException {
        LOGGER.info("-------Cleaning target---------");
        ProcessBuilder pb = new ProcessBuilder();
        if(isWindows())
            pb.command("cmd.exe", "/c", "mvn", "clean");
        else
            pb.command("bash", "-c", "mvn clean");
        pb.directory(new File(this.name));
        Process process = pb.start();
        BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while((line = br.readLine()) != null)
            LOGGER.info(line);
        process.waitFor();
    }

    public List<Pair<String,?>> obtainCoverageFilesAndEntryPoints(){
        List<Pair<String,?>> coverageFiles = new LinkedList<>();
        for(Map<String, ?> m : properties){
            if(m.get("entryPoint") instanceof String){
                coverageFiles.add(new Pair<>("artifacts/results/" + getProjectDir() + timeStamp + "/" + m.get("name") + ".xml", m.get("entryPoint")));
            }
            else{
                coverageFiles.add(new Pair<String, ArrayList>("artifacts/results/" + getProjectDir() + timeStamp + "/" + m.get("name") + ".xml", (ArrayList) m.get("entryPoint")));
            }
        }

        return coverageFiles;
    }

    public String getTestJar() {
        return testJar;
    }

    public String getMainJar() {
        return mainJar;
    }

    public static Optional<RepoTool> obtainTool(String folderName){
        try {
            Yaml yaml = new Yaml();
            InputStream inputStream = new FileInputStream("artifacts/configs/" + folderName + "/" + folderName + ".yaml");
            Map<String, String> data = yaml.load(inputStream);
            return Optional.of(new RepoTool(data.get("name"), data.get("URL"), data.get("checkoutID"), data.get("patchName"), data.getOrDefault("subProject", ""), data.getOrDefault("mvnOptions", ""),  data.getOrDefault("mainJar", ""),  data.getOrDefault("testJar", "")));
        }
        catch(IOException e){
            LOGGER.error("IOException: " + e.getMessage());
        }
        LOGGER.error("Could not obtain yaml file!");
        return Optional.empty();
    }

    private void moveJars() throws IOException {
        Path sourceDir = Paths.get(System.getProperty("user.dir"), getProjectDir(), "target");
        Path targetDir = Paths.get(System.getProperty("user.dir"), "artifacts", "output");
        moveFiles(sourceDir, targetDir, "*.jar");
    }

    private void moveFiles(Path sourceDir, Path targetDir, String glob) throws IOException {
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(sourceDir, glob)) {
                for (Path source: dirStream) {
                Files.move(
                        source,
                        targetDir.resolve(source.getFileName()),
                        StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private void moveJacoco(String property, long timeElapsed) throws IOException{
        String projectDir = getProjectDir();
        String directoryPath = System.getProperty("user.dir") + "/artifacts/results/" + projectDir + timeStamp;
        String jacocoPath = System.getProperty("user.dir") + "/" + projectDir + "/target/site/jacoco/jacoco.xml";
        String jacocoTargetPath = directoryPath + "/" + property + ".xml";
        String statisticsPath = System.getProperty("user.dir") + "/" + projectDir + "/target/site/jacoco/index.html";
        String statisticsTargetPath = directoryPath + "/" + property + ".html";
        if(projectDir.contains("/")){
            String [] directories = projectDir.split("/");
            String rootDirectoryPath = System.getProperty("user.dir") + "/artifacts/results/" + directories[0];
            File rootDir = new File(rootDirectoryPath);
            rootDir.mkdir();
        }
        File directory = new File(directoryPath);
        directory.mkdir();
        Files.move(
                Paths.get(jacocoPath),
                Paths.get(jacocoTargetPath),
                StandardCopyOption.REPLACE_EXISTING);
        Files.move(
                Paths.get(statisticsPath),
                Paths.get(statisticsTargetPath),
                StandardCopyOption.REPLACE_EXISTING);
        double timeElapsedInSeconds = (double) timeElapsed / 1_000_000_000;
        try (FileWriter fileWriter = new FileWriter(statisticsTargetPath, true); BufferedWriter bufferedWriter = new BufferedWriter(fileWriter)) {
            bufferedWriter.append("<html><section><h1> Total Time Elapsed: ").append(String.valueOf(timeElapsedInSeconds)).append(" seconds</h1></section></html>");
            bufferedWriter.flush();
        }
    }

    public void moveOutput() throws Exception {
        moveFiles(
            Paths.get(System.getProperty("user.dir"), "/output"), // src
            Paths.get(System.getProperty("user.dir"), "/artifacts/results/", getProjectDir(), timeStamp), // dst
            "*.*"  // g;ob
        );
    }

    private boolean isWindows() {
        return System.getProperty("os.name")
                .toLowerCase().startsWith("windows");
    }

    private String getProjectDir() {
        return (subProject.equals("")) ? name : (name + "/" + subProject);
    }
}
