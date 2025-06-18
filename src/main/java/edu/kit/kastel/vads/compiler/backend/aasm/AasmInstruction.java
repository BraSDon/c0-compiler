package edu.kit.kastel.vads.compiler.backend.aasm;

import edu.kit.kastel.vads.compiler.backend.codegen.Instruction;

public record AasmInstruction(String value) implements Instruction {
    @Override
    public String toString() {
        return value;
    }
}
