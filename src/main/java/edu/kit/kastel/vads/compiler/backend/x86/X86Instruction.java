package edu.kit.kastel.vads.compiler.backend.x86;

import edu.kit.kastel.vads.compiler.backend.codegen.Instruction;
import edu.kit.kastel.vads.compiler.backend.codegen.Operand;

import java.util.Arrays;

public record X86Instruction(X86Operation operation, Operand... operands) implements Instruction {

    @Override
    public String toString() {
        // Format: "OPCODE OP1, OP2, ..., OPn"
        return operands.length == 0
                ? operation.getMnemonic()
                : operation.getMnemonic() + " " + String.join(", ", operandsString());
    }

    // TODO: Should maybe be executed on creation
    public boolean validate() {
        return operation.validateOperands(Arrays.stream(operands)
                .map(Operand::kind)
                .toList());
    }

    private String[] operandsString() {
        return Arrays.stream(operands)
                .map(Operand::toString)
                .toArray(String[]::new);
    }
}
