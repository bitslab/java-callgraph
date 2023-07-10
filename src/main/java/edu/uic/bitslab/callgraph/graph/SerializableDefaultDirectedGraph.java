package edu.uic.bitslab.callgraph.graph;

import org.jgrapht.graph.DefaultDirectedGraph;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;


public class SerializableDefaultDirectedGraph<V, E> extends DefaultDirectedGraph<V, E> implements Serializable {
    public SerializableDefaultDirectedGraph(Class<? extends E> edgeClass) {
        super(edgeClass);
    }

    private void writeObject(ObjectOutputStream oos) throws IOException {
        oos.defaultWriteObject();
    }

    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        ois.defaultReadObject();
    }
}
