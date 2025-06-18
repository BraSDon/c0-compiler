package edu.kit.kastel.vads.compiler.backend.codegen;

import edu.kit.kastel.vads.compiler.backend.regalloc.StackSlot;

public record MemoryOperand(StackSlot stackSlot) implements Operand {
    @Override
    public Kind kind() {
        return Kind.MEMORY;
    }

    @Override
    public String toString() {
        return stackSlot.toString();
    }
}
