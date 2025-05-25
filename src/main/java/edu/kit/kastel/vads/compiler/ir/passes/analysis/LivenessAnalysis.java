package edu.kit.kastel.vads.compiler.ir.passes.analysis;

import edu.kit.kastel.vads.compiler.ir.IrGraph;
import edu.kit.kastel.vads.compiler.ir.node.BinaryOperationNode;
import edu.kit.kastel.vads.compiler.ir.node.ConstIntNode;
import edu.kit.kastel.vads.compiler.ir.node.Node;
import edu.kit.kastel.vads.compiler.ir.node.ReturnNode;
import edu.kit.kastel.vads.compiler.ir.util.NodeSupport;

import java.util.List;
import java.util.Set;
import java.util.HashSet;

public class LivenessAnalysis extends BackwardsControlFlow<Set<Node>, Set<Node>> {

    @Override
    public void analyze(IrGraph graph) {
        super.analyze(graph);
        assert getResult().get(graph.nodesInReversePostOrder().get(0)).inValue().isEmpty()
                : "Nothing should be live at the start of the graph.";
    }

    @Override
    public Set<Node> computeInValue(Node node, Set<Node> liveOut) {
        Set<Node> defs = defs(node);
        Set<Node> uses = uses(node);

        // liveOut - defs
        Set<Node> tmp = new HashSet<>(liveOut);
        tmp.removeAll(defs);

        // liveIn = uses + tmp
        Set<Node> liveIn = new HashSet<>(uses);
        liveIn.addAll(tmp);
        return liveIn;
    }

    @Override
    public Set<Node> computeOutValue(Node node, List<Set<Node>> liveIn) {
        Set<Node> liveOut = new HashSet<>();
        for (Set<Node> inValue : liveIn) {
            liveOut.addAll(inValue);
        }
        return liveOut;

    }

    private Set<Node> defs(Node node) {
        switch (node) {
            case BinaryOperationNode _,ConstIntNode _ -> {
                return Set.of(node);
            }
            default -> {
                return Set.of();
            }
        }
    }

    private Set<Node> uses(Node node) {
        switch (node) {
            case BinaryOperationNode _ -> {
                Set<Node> uses = new HashSet<>();
                uses.add(NodeSupport.predecessorSkipProj(node, BinaryOperationNode.LEFT));
                uses.add(NodeSupport.predecessorSkipProj(node, BinaryOperationNode.RIGHT));
                return uses;
            }
            case ReturnNode _ -> {
                Node result = NodeSupport.predecessorSkipProj(node, ReturnNode.RESULT);
                return Set.of(result);
            }
            default -> {
                return Set.of();
            }
        }
    }

}
