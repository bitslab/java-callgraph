/*
 * Copyright (c) 2011 - Georgios Gousios <gousiosg@gmail.com>
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package gr.gousiosg.javacg.stat;

import gr.gousiosg.javacg.stat.support.Arguments;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.InputMismatchException;

/**
 * Constructs a callgraph out of a JAR archive. Can combine multiple archives
 * into a single call graph.
 *
 * @author Georgios Gousios <gousiosg@gmail.com>
 */
public class JCallGraph {

    private static final Logger LOGGER = LoggerFactory.getLogger(JCallGraph.class);
    private static final String REACHABILITY = "reachability";
    private static final String DELIMITER = "-";
    private static final String DOT_SUFFIX = ".dot";

    public static void main(String[] args) {
        try {
            LOGGER.info("Starting java-cg!");

            /* Setup arguments */
            Arguments arguments = new Arguments(args);

            /* Create callgraph */
            Graph<String, DefaultEdge>  graph = GraphHelper.staticCallgraph(arguments.getJars());

            /* Should we store the graph in a file? */
            if (arguments.maybeOutput().isPresent()) {
                GraphHelper.writeGraph(graph, asDot(arguments.maybeOutput().get()));
            }

            /* Should we compute reachability from the entry point? */
            if (arguments.maybeEntryPoint().isPresent()) {
                Graph<String, DefaultEdge>  subgraph = GraphHelper.reachability(graph, arguments.maybeEntryPoint().get(), arguments.maybeDepth());

                /* Should we store the reachability subgraph in a file? */
                if (arguments.maybeOutput().isPresent()) {
                    String subgraphOutputName = arguments.maybeOutput().get() + DELIMITER + REACHABILITY;

                    /* Does this subgraph's reachability have a depth? */
                    if (arguments.maybeDepth().isPresent()) {
                        subgraphOutputName = subgraphOutputName + DELIMITER + arguments.maybeDepth().get();
                    }

                    GraphHelper.writeGraph(subgraph, asDot(subgraphOutputName));
                }
            }

        } catch (InputMismatchException e) {
            LOGGER.error("Unable to load callgraph: " + e.getMessage());
            System.exit(1);
        }

        LOGGER.info("java-cg is finished! Enjoy!");
    }

    private static String asDot(String name) {
        return name.endsWith(DOT_SUFFIX) ? name : (name + DOT_SUFFIX);
    }

}
