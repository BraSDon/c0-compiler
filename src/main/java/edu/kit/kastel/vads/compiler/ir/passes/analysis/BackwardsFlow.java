package edu.kit.kastel.vads.compiler.ir.passes.analysis;

import edu.kit.kastel.vads.compiler.ir.IrGraph;
import edu.kit.kastel.vads.compiler.ir.node.Node;
import edu.kit.kastel.vads.compiler.ir.passes.IrAnalysisPass;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.Collections;

public abstract class BackwardsFlow<InValue, OutValue>
        implements IrAnalysisPass<Map<Node, BackwardsFlow.BackwardsFlowResult<InValue, OutValue>>> {

    // Caches the last analyzed inValues from each node's successors
    private Map<Node, List<InValue>> previousSuccIn = new HashMap<>();
    private final Map<Node, OutValue> out = new HashMap<>();
    private final Map<Node, InValue> in = new HashMap<>();

    public record BackwardsFlowResult<IV, OV>(IV inValue, OV outValue) {
    }

    public Map<Node, BackwardsFlowResult<InValue, OutValue>> getResult() {
        return Collections.unmodifiableMap(
                in.keySet().stream()
                        .collect(Collectors.toMap(
                                key -> key,
                                key -> new BackwardsFlowResult<>(in.get(key), out.get(key)))));
    }

    public void analyze(IrGraph graph) {
        analyzeNode(graph, graph.endBlock());
    }

    private void analyzeNode(IrGraph graph, Node node) {
        List<InValue> inValues = successors(node).stream()
                .map(successorNode -> in.get(successorNode))
                .collect(Collectors.toList());

        // Exit early if inValues have not changed
        if (Objects.equals(previousSuccIn.get(node), inValues)) {
            return;
        }

        OutValue outValue = computeOutValue(node, inValues);
        out.put(node, outValue);

        InValue inValue = computeInValue(node, outValue);
        in.put(node, inValue);

        previousSuccIn.put(node, inValues);

        for (Node predecessor : predecessors(node)) {
            analyzeNode(graph, predecessor);
        }
    }

    public abstract List<Node> predecessors(Node node);

    public abstract List<Node> successors(Node node);

    public abstract OutValue computeOutValue(Node node, List<InValue> inValues);

    public abstract InValue computeInValue(Node node, OutValue outValue);
}
