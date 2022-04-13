package edu.uic.bitslab.callgraph;

import gr.gousiosg.javacg.stat.graph.StaticCallgraph;
import org.jgrapht.traverse.DepthFirstIterator;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.util.Iterator;

public class GetBest {
    private StaticCallgraph callgraph;

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        GetBest o = new GetBest(args[0]);
        o.run();
    }

    public GetBest(String objectFile) throws IOException, ClassNotFoundException {
        try (ObjectInput ois = new ObjectInputStream(new FileInputStream(objectFile))) {
            callgraph = (StaticCallgraph) ois.readObject();
        }
    }

    public void run() {
        Iterator<String> iter = new DepthFirstIterator<>(callgraph.graph);
        while (iter.hasNext()) {
            String vertex = iter.next();
            System.out.println("Vertex " + vertex + " is connected to: " + callgraph.graph.edgesOf(vertex).toString());
        }
    }


}

