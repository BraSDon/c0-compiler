package edu.kit.kastel.vads.compiler.backend.codegen;

public record ImmediateOperand(int value) implements Operand {
    @Override
    public Kind kind() {
        return Kind.IMMEDIATE;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
