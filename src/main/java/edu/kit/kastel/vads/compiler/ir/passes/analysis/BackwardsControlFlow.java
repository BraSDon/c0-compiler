package edu.kit.kastel.vads.compiler.ir.passes.analysis;

import edu.kit.kastel.vads.compiler.ir.IrGraph;
import edu.kit.kastel.vads.compiler.ir.node.Node;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class BackwardsControlFlow<InValue, OutValue> extends BackwardsFlow<InValue, OutValue> {

    private final Map<IrGraph, List<Node>> graphNodeOrder = new HashMap<>();

    @Override
    public void analyze(IrGraph graph) {
        this.graphNodeOrder.put(graph, graph.nodesInReversePostOrder());
        super.analyze(graph);
    }

    @Override
    public List<Node> predecessors(Node node) {
        List<Node> nodes = graphNodeOrder.get(node.graph());
        int idx = nodes.indexOf(node);
        return (idx > 0) ? List.of(nodes.get(idx - 1)) : Collections.emptyList();
    }

    @Override
    public List<Node> successors(Node node) {
        List<Node> nodes = graphNodeOrder.get(node.graph());
        int idx = nodes.indexOf(node);
        return (idx < nodes.size() - 1) ? List.of(nodes.get(idx + 1)) : Collections.emptyList();
    }
}
