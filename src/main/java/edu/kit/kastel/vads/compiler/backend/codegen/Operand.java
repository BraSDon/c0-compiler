package edu.kit.kastel.vads.compiler.backend.codegen;

public sealed interface Operand permits RegisterOperand, MemoryOperand, ImmediateOperand {
    enum Kind {
        REGISTER, MEMORY, IMMEDIATE
    }

    Kind kind();

    String toString();
}
