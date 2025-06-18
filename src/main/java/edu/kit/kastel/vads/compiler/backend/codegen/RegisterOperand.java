package edu.kit.kastel.vads.compiler.backend.codegen;

import edu.kit.kastel.vads.compiler.backend.regalloc.Register;

public record RegisterOperand(Register register) implements Operand {
    @Override
    public Kind kind() {
        return Kind.REGISTER;
    }

    @Override
    public String toString() {
        return register.toString();
    }
}
