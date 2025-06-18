package edu.kit.kastel.vads.compiler.backend.x86;

import edu.kit.kastel.vads.compiler.ir.IrGraph;
import edu.kit.kastel.vads.compiler.ir.node.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class StackSlotAllocator {
    private int currentStackOffset;
    private final Map<Node, StackSlot> slots = new HashMap<>();
    private final int slotSizeBytes = 4; // L1 integers are 32-bit

    /**
     * Assigns stack slots to all value-producing nodes in the graph.
     * Traverses the graph in a reverse post-order (processing inputs before a
     * node).
     * 
     * @param graph The IR graph.
     * @return A map from IR nodes to their assigned StackSlot.
     */
    public Map<Node, StackSlot> assignSlots(IrGraph graph) {
        this.currentStackOffset = 0;
        this.slots.clear();

        Set<Node> visited = new HashSet<>();
        // EndBlock itself doesn't get a slot but is the start of our reverse traversal.
        visited.add(graph.endBlock());

        // Recursively traverse and assign slots
        scan(graph.endBlock(), visited);

        return Map.copyOf(this.slots);
    }

    private void scan(Node node, Set<Node> visited) {
        // Process predecessors first
        for (Node predecessor : node.predecessors()) {
            if (visited.add(predecessor)) {
                scan(predecessor, visited);
            }
        }

        // Process 'node' itself.
        if (needsStackSlot(node)) {
            if (!this.slots.containsKey(node)) {
                this.currentStackOffset += slotSizeBytes;
                this.slots.put(node, new StackSlot(this.currentStackOffset));
            }
        }
    }

    public int getTotalAllocatedSlotSize() {
        return this.currentStackOffset;
    }

    private static boolean needsStackSlot(Node node) {
        return !(node instanceof ProjNode ||
                node instanceof StartNode ||
                node instanceof Block ||
                node instanceof ReturnNode);
    }
}
