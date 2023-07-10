package edu.uic.bitslab.callgraph.support;

import edu.uic.bitslab.callgraph.config.SUTConfig;
import gr.gousiosg.javacg.dyn.Pair;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class RepoTool {

    private static final Logger LOGGER = LoggerFactory.getLogger(RepoTool.class);
    final private String timeStamp;
    final private SUTConfig sutConfig;

    public RepoTool(SUTConfig sutConfig){
        this(sutConfig, String.valueOf(java.time.LocalDateTime.now()).replace(':', '_'));
    }

    public RepoTool(SUTConfig sutConfig, String timeStamp){
        this.sutConfig = sutConfig;
        this.timeStamp = timeStamp;
    }

    public RepoTool(String name) {
        this(name, String.valueOf(java.time.LocalDateTime.now()).replace(':', '_'));
    }

    public RepoTool(String name, String timeStamp) {
        this(
            SUTConfig.fromProjectName(name),
            timeStamp
        );
    }

    public SUTConfig getSUTConfig() {
        return sutConfig;
    }

    public void cloneRepo() throws GitAPIException, JGitInternalException {
        Git git = Git.cloneRepository()
                .setDirectory(new File(sutConfig.name))
                .setURI(sutConfig.URL)
                .call();

        git.checkout()
                .setName(sutConfig.checkoutID)
                .call();
    }

    public void applyPatch() throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder();
        if(isWindows())
            pb.command("cmd.exe", "/c", "git", "apply", sutConfig.patchName, "--directory", sutConfig.name);
        else
            pb.command("bash", "-c", "patch -p1 -d " + sutConfig.name + " < " + sutConfig.patchName);
        Process process = pb.start();
        process.waitFor();
    }

    public void buildJars() throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder();
        if(isWindows())
            pb.command("cmd.exe", "/c", "mvn", "install", "-DskipTests");
        else
            pb.command("bash", "-c", "mvn install -DskipTests");
        pb.directory(new File(sutConfig.name));
        Process process = pb.start();
        BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while((line = br.readLine()) != null)
            LOGGER.info(line);
        process.waitFor();
        copyJars();
    }

    public void testProperty(String property) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder();
        if(isWindows())
            pb.command("cmd.exe", "/c", "mvn", "test", sutConfig.mvnOptions, "-Dtest=" + property);
        else
            pb.command("bash", "-c", "mvn test " + sutConfig.mvnOptions + " -Dtest=" + property);
        pb.directory(new File(sutConfig.name));
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
        pb.directory(new File(sutConfig.name));
        Process process = pb.start();
        BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while((line = br.readLine()) != null)
            LOGGER.info(line);
        process.waitFor();
    }

    public List<Pair<String,?>> obtainCoverageFilesAndEntryPoints(){
        List<Pair<String,?>> coverageFiles = new LinkedList<>();
        for(SUTConfig.Property p : sutConfig.properties){
            String projectDir = sutConfig.getProjectDir();

            if(!projectDir.contains("/"))
                coverageFiles.add(new Pair<>("artifacts/results/" + projectDir + "/"+ projectDir + timeStamp + "/" + p.name + ".xml", p.entryPoint));
            else
                coverageFiles.add(new Pair<>("artifacts/results/" + projectDir + timeStamp + "/" + p.name + ".xml", p.entryPoint));
        }

        return coverageFiles;
    }

    private void copyJars() throws IOException {
        Path sourceDir = Paths.get(System.getProperty("user.dir"), sutConfig.getProjectDir(), "target");
        Path targetDir = Paths.get(System.getProperty("user.dir"), "artifacts", "output", sutConfig.getProjectDir());
        File validateDirectory = targetDir.toFile();
        if(!validateDirectory.exists() && !validateDirectory.mkdirs()) {
            throw new IOException("Unable to create the target directory");
        }
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

    private void copyFiles(Path sourceDir, Path targetDir) throws IOException {
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(sourceDir)) {
            for (Path source: dirStream) {
                Files.copy(
                        source,
                        targetDir.resolve(source.getFileName()),
                        StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }


    private void moveJacoco(String property, long timeElapsed) throws IOException{
        String projectDir = sutConfig.getProjectDir();
        String directoryPath = System.getProperty("user.dir") + "/artifacts/results/" + projectDir + timeStamp;
        if(!projectDir.contains("/"))
            directoryPath = System.getProperty("user.dir") + "/artifacts/results/" + projectDir + "/" + projectDir + timeStamp;
        String jacocoPath = System.getProperty("user.dir") + "/" + projectDir + "/target/site/jacoco/jacoco.xml";
        String jacocoTargetPath = directoryPath + "/" + property + ".xml";
        String statisticsPath = System.getProperty("user.dir") + "/" + projectDir + "/target/site/jacoco/index.html";
        String statisticsTargetPath = directoryPath + "/" + property + ".html";
        if(projectDir.contains("/")){
            String [] directories = projectDir.split("/");
            String rootDirectoryPath = System.getProperty("user.dir") + "/artifacts/results/" + directories[0];
            File rootDir = new File(rootDirectoryPath);
            if(!rootDir.exists() && !rootDir.mkdir()) {
                throw new IOException("Unable to create rootDir");
            }
        }
        File directory = new File(directoryPath);
        if(!directory.exists() && !directory.mkdir()) {
            throw new IOException("Unable to create directory");
        }
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
        String projectDirectory = sutConfig.getProjectDir();
        if(!projectDirectory.contains("/"))
            projectDirectory = projectDirectory + "/" + projectDirectory;
        copyFiles(
            Paths.get(System.getProperty("user.dir"), "/output"), // src
            Paths.get(System.getProperty("user.dir"), "/artifacts/results/", projectDirectory + timeStamp) // dst
        );
    }

    private boolean isWindows() {
        return System.getProperty("os.name")
                .toLowerCase().startsWith("windows");
    }
}
