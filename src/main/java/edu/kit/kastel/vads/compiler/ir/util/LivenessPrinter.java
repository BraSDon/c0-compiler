package edu.kit.kastel.vads.compiler.ir.util;

import edu.kit.kastel.vads.compiler.ir.node.Node;
import edu.kit.kastel.vads.compiler.ir.IrGraph;
import edu.kit.kastel.vads.compiler.ir.passes.analysis.BackwardsFlow;

import java.util.Map;
import java.util.Set;

public class LivenessPrinter {
    public static void printLiveness(IrGraph graph,
            Map<Node, BackwardsFlow.BackwardsFlowResult<Set<Node>, Set<Node>>> liveness) {
        System.out.println("Liveness Analysis Results:");
        System.out.printf("%-20s | %-40s | %-40s%n", "Node", "Live In", "Live Out");
        System.out.println("-".repeat(100));
        for (Node node : graph.nodesInReversePostOrder()) {
            Set<Node> liveIn = liveness.get(node).inValue();
            Set<Node> liveOut = liveness.get(node).outValue();
            System.out.printf("%-20s | %-40s | %-40s%n",
                    node,
                    liveIn.isEmpty() ? "[]" : liveIn.toString(),
                    liveOut.isEmpty() ? "[]" : liveOut.toString());
        }
        System.out.println();
    }
}
