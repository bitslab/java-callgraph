package edu.uic.bitslab.callgraph.support;

import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class GitArguments {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitArguments.class);
    private static final String CONFIG_NAME = "c";
    private static final String CONFIG_NAME_LONG = "config";
    private Optional<String> maybeConfig = Optional.empty();

    public GitArguments(String[] args){
        LOGGER.info("Parsing command line arguments...");
        CommandLineParser parser = new DefaultParser();
        Options options = getOptions();
        CommandLine cmd;
        try{
            cmd = parser.parse(options, args);
            if(cmd.hasOption(CONFIG_NAME)){
                String configFile = cmd.getOptionValue(CONFIG_NAME);
                maybeConfig = Optional.of(configFile);
            }
        }
        catch(ParseException e){
            LOGGER.error("Configuration file required to run 'git'");
            LOGGER.error("Please chose a valid configuration found inside 'artifacts/configs'");
            System.exit(1);
        }
    }

    private static Options getOptions() {
        Options options = new Options();

        options.addOption(
                Option.builder(CONFIG_NAME)
                        .longOpt(CONFIG_NAME_LONG)
                        .hasArg(true)
                        .desc("[REQUIRED] specify configuration to test for coverage")
                        .required(true)
                        .build());

        return options;
    }

    public Optional<String> maybeGetConfig(){ return maybeConfig; }
}
