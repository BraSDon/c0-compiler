package edu.kit.kastel.vads.compiler.ir.passes.analysis;

import edu.kit.kastel.vads.compiler.ir.node.Node;
import edu.kit.kastel.vads.compiler.ir.node.BinaryOperationNode;
import edu.kit.kastel.vads.compiler.backend.regalloc.RegisterRequirement;
import edu.kit.kastel.vads.compiler.ir.IrGraph;

import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.jgrapht.Graph;
import org.jgrapht.graph.SimpleGraph;
import org.jgrapht.graph.DefaultEdge;

public class InterferenceGraph {
    // Undirected, unweighted and irreflexive graph
    private final Graph<Node, DefaultEdge> g;
    private Integer maxColor = 0;

    public InterferenceGraph(IrGraph graph) {
        var livenessAnalysis = new LivenessAnalysis();
        livenessAnalysis.analyze(graph);
        var liveness = livenessAnalysis.getResult();

        this.g = new SimpleGraph<>(DefaultEdge.class);
        List<Node> nodes = graph.nodesInReversePostOrder().stream()
                .filter(RegisterRequirement::needsRegister)
                .toList();
        for (Node node : nodes) {
            g.addVertex(node);
        }

        for (Node node : nodes) {
            var liveOut = liveness.get(node).outValue();

            switch (node) {
                case BinaryOperationNode _ -> {
                    // All nodes in liveOut != node are interfering with node
                    liveOut.stream()
                            .filter(otherNode -> !otherNode.equals(node))
                            .filter(RegisterRequirement::needsRegister)
                            .forEach(otherNode -> g.addEdge(node, otherNode));
                }
                default -> {
                }
            }
        }
    }

    // TODO: Implement pre-coloring for operations that require specific registers
    public Map<Node, Integer> color() {
        Node[] elimOrder = simplicialEliminationOrder();
        int maxDegree = maxDegree();
        Map<Node, Integer> colorMap = new HashMap<>();
        for (Node node : elimOrder) {
            Integer color = lowestUnusedColorInNeighborhood(node, colorMap);
            colorMap.put(node, color);
        }
        maxColor = colorMap.values().stream().max(Integer::compare).orElse(0);
        assert maxColor <= maxDegree + 1
                : "Coloring sub-optimal, max color used exceeds max degree + 1";
        return colorMap;
    }

    public int getMaxColor() {
        return maxColor;
    }

    private Integer lowestUnusedColorInNeighborhood(Node node, Map<Node, Integer> colorMap) {
        Set<Integer> colorsUsedByNeighbor = new HashSet<>();
        for (DefaultEdge edge : g.edgesOf(node)) {
            Node neighbor = g.getEdgeSource(edge).equals(node) ? g.getEdgeTarget(edge) : g.getEdgeSource(edge);
            if (colorMap.containsKey(neighbor)) {
                colorsUsedByNeighbor.add(colorMap.get(neighbor));
            }
        }

        int color = 0;
        while (colorsUsedByNeighbor.contains(color)) {
            color++;
        }
        return color;
    }

    private Node[] simplicialEliminationOrder() {
        Node[] order = new Node[g.vertexSet().size()];
        Map<Node, Integer> weight = new HashMap<>();
        Set<Node> W = new HashSet<>(g.vertexSet());

        for (Node v : W) {
            weight.put(v, 0);
        }

        int n = W.size();
        for (int i = 0; i < n; i++) {
            // Find node with maximum weight in W
            Node maxNode = null;
            for (Node v : W) {
                if (maxNode == null || weight.get(v) > weight.get(maxNode)) {
                    maxNode = v;
                }
            }
            // Store the node in the order
            order[i] = maxNode;

            // For all u in W ∩ N(v), increment weight
            for (DefaultEdge e : g.edgesOf(maxNode)) {
                Node u = g.getEdgeSource(e).equals(maxNode) ? g.getEdgeTarget(e) : g.getEdgeSource(e);
                if (W.contains(u) && !u.equals(maxNode)) {
                    weight.put(u, weight.get(u) + 1);
                }
            }
            W.remove(maxNode);
        }
        return order;
    }

    private int maxDegree() {
        return g.vertexSet().stream()
                .mapToInt(g::degreeOf)
                .max()
                .orElse(0);
    }
}
