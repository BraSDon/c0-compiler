package edu.kit.kastel.vads.compiler.ir;

import edu.kit.kastel.vads.compiler.ir.node.Block;
import edu.kit.kastel.vads.compiler.ir.node.Node;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.SequencedSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;

public class IrGraph {
    private final Map<Node, SequencedSet<Node>> successors = new IdentityHashMap<>();
    private final Block startBlock;
    private final Block endBlock;
    private final String name;

    public IrGraph(String name) {
        this.name = name;
        this.startBlock = new Block(this);
        this.endBlock = new Block(this);
    }

    public void registerSuccessor(Node node, Node successor) {
        this.successors.computeIfAbsent(node, _ -> new LinkedHashSet<>()).add(successor);
    }

    public void removeSuccessor(Node node, Node oldSuccessor) {
        this.successors.computeIfAbsent(node, _ -> new LinkedHashSet<>()).remove(oldSuccessor);
    }

    /// {@return the set of nodes that have the given node as one of their inputs}
    public Set<Node> successors(Node node) {
        SequencedSet<Node> successors = this.successors.get(node);
        if (successors == null) {
            return Set.of();
        }
        return Set.copyOf(successors);
    }

    public Block startBlock() {
        return this.startBlock;
    }

    public Block endBlock() {
        return this.endBlock;
    }

    /// {@return the name of this graph}
    public String name() {
        return name;
    }

    public List<Node> nodesInReversePostOrder() {
        List<Node> result = new ArrayList<>();
        Set<Node> visited = new HashSet<>();
        visited.add(endBlock);
        IrGraph.scan(endBlock, visited, result);
        return result;
    }

    private static void scan(Node node, Set<Node> visited, List<Node> result) {
        for (Node predecessor : node.predecessors()) {
            if (!visited.contains(predecessor)) {
                visited.add(predecessor);
                scan(predecessor, visited, result);
            }
        }
        result.add(node);
    }

    public Set<Node> nodes() {
        Set<Node> nodes = new LinkedHashSet<>();
        for (Node node : this.successors.keySet()) {
            nodes.add(node);
            nodes.addAll(this.successors.get(node));
        }
        nodes.add(startBlock);
        nodes.add(endBlock);
        return nodes;
    }
}
