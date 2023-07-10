package edu.uic.bitslab.callgraph.support;

import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class TestArguments {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestArguments.class);

    private static final String FILE_NAME = "f";
    private static final String FILE_NAME_LONG = "file";
    private static final String CONFIG_NAME = "c";
    private static final String CONFIG_NAME_LONG = "config";
    private static final String DEPTH_INPUT = "d";
    private static final String DEPTH_INPUT_LONG = "depth";
    private static final String ANCESTRY_INPUT = "a";
    private static final String ANCESTRY_INPUT_LONG = "ancestry";

    private Optional<String> bytecodeFile;
    private Optional<String> maybeConfig = Optional.empty();
    private Optional<Integer> maybeDepth = Optional.empty();
    private Optional<Integer> maybeAncestry = Optional.empty();

    public TestArguments(String[] args){
        LOGGER.info("Parsing command line arguments...");

        /* Setup cmdline argument parsing */
        CommandLineParser parser = new DefaultParser();
        Options options = getOptions();
        CommandLine cmd;

        try{
            cmd = parser.parse(options, args);

            /*Parse bytecode file */
            if (cmd.hasOption(FILE_NAME)){
                bytecodeFile = Optional.of(cmd.getOptionValue(FILE_NAME));
            }

            if(cmd.hasOption(CONFIG_NAME)){
                this.maybeConfig = Optional.of(cmd.getOptionValue(CONFIG_NAME));
            }
            /* Parse ancestry */
            if (cmd.hasOption(ANCESTRY_INPUT)) {
                String val = cmd.getOptionValue(ANCESTRY_INPUT);
                try {
                    this.maybeAncestry = Optional.of(Integer.parseInt(val));
                } catch (NumberFormatException e) {
                    LOGGER.error("---> " + val + " <---");
                    LOGGER.error("Please specify a valid integer for depth!");
                    System.exit(1);
                }
            }

            /* Parse depth */
            if (cmd.hasOption(DEPTH_INPUT)) {
                String val = cmd.getOptionValue(DEPTH_INPUT);
                try {
                    this.maybeDepth = Optional.of(Integer.parseInt(val));
                } catch (NumberFormatException e) {
                    LOGGER.error("---> " + val + " <---");
                    LOGGER.error("Please specify a valid integer for depth!");
                    System.exit(1);
                }
            }
        }
        catch(ParseException pe){
            LOGGER.error("Error parsing command-line arguments: " + pe.getMessage());
            LOGGER.error("Please, follow the instructions below:");
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("Log messages to sequence diagrams converter", options);
            System.exit(1);
        }

    }

    private static Options getOptions() {
        Options options = new Options();
        options.addOption(
                Option.builder(CONFIG_NAME)
                .longOpt(CONFIG_NAME_LONG)
                .hasArg(true)
                .desc("[REQUIRED] specify the config folder")
                .required(true)
                .build());

        options.addOption(
                Option.builder(FILE_NAME)
                        .longOpt(FILE_NAME_LONG)
                        .hasArg(true)
                        .desc("[REQUIRED] specify the bytecode file")
                        .required(true)
                        .build());


        options.addOption(
                Option.builder(DEPTH_INPUT)
                        .longOpt(DEPTH_INPUT_LONG)
                        .hasArg(true)
                        .desc("[OPTIONAL] specify a depth to explore graph to")
                        .required(false)
                        .build());


        options.addOption(
                Option.builder(ANCESTRY_INPUT)
                        .longOpt(ANCESTRY_INPUT_LONG)
                        .hasArg(true)
                        .desc("[OPTIONAL] specify a depth to traverse the ancestry of an entrypoint")
                        .required(false)
                        .build());

        return options;
    }

    public Optional<String> maybeBytecodeFile(){ return bytecodeFile; }

    public Optional<Integer> maybeDepth() {
        return maybeDepth;
    }

    public Optional<String> maybeGetConfig() {return maybeConfig;}

    public Optional<Integer> maybeAncestry() {
        return maybeAncestry;
    }
}
