package edu.kit.kastel.vads.compiler.backend.x86;

import edu.kit.kastel.vads.compiler.ir.IrGraph;
import edu.kit.kastel.vads.compiler.ir.node.Node;
import edu.kit.kastel.vads.compiler.ir.passes.analysis.InterferenceGraph;
import edu.kit.kastel.vads.compiler.backend.regalloc.RegisterAllocator;
import edu.kit.kastel.vads.compiler.backend.regalloc.StackSlot;
import edu.kit.kastel.vads.compiler.backend.regalloc.Location;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class X86RegisterAllocator implements RegisterAllocator {
    public Map<Node, Location> allocateRegisters(IrGraph graph) {
        var interferenceGraph = new InterferenceGraph(graph);
        Map<Node, Integer> coloring = interferenceGraph.color();

        List<X86Register> registers = new ArrayList<>(X86Register.allocatableGPRs());
        // boolean spillNeeded = interferenceGraph.getMaxColor() >= registers.size();

        return naiveAllocation(coloring, registers);
    }

    // All colors up to register.size() are used for registers, the rest is spilled
    private Map<Node, Location> naiveAllocation(Map<Node, Integer> coloring, List<X86Register> registers) {
        Map<Node, Location> allocation = new HashMap<>();
        Map<Integer, StackSlot> stackSlots = getStackSlots(coloring, registers.size());
        for (Map.Entry<Node, Integer> entry : coloring.entrySet()) {
            Node node = entry.getKey();
            int color = entry.getValue();
            if (color < registers.size()) {
                allocation.put(node, registers.get(color));
            } else {
                allocation.put(node, stackSlots.get(color));
            }
        }
        return allocation;
    }

    private Map<Integer, StackSlot> getStackSlots(Map<Node, Integer> coloring, int numRegisters) {
        int stackOffset = 0;
        Map<Integer, StackSlot> stackSlots = new HashMap<>();
        for (Map.Entry<Node, Integer> entry : coloring.entrySet()) {
            int color = entry.getValue();
            if (color >= numRegisters) {
                stackSlots.putIfAbsent(color, new StackSlot(stackOffset));
                stackOffset += StackSlot.SLOT_SIZE;
            }
        }
        return stackSlots;
    }
}
