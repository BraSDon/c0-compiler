package edu.kit.kastel.vads.compiler.backend.x86;

import edu.kit.kastel.vads.compiler.backend.codegen.Operand;
import static edu.kit.kastel.vads.compiler.backend.codegen.Operand.Kind.*;

import java.util.List;

/**
 * Represents common x86-64 operation mnemonics.
 * This is a non-exhaustive list, extend as needed.
 */
public enum X86Operation {
    // Data Transfer
    MOV("mov"), // Move
    LEA("lea"), // Load Effective Address
    PUSH("push"), // Push onto stack
    POP("pop"), // Pop from stack

    // Arithmetic
    ADD("add"), // Add
    SUB("sub"), // Subtract
    INC("inc"), // Increment
    DEC("dec"), // Decrement
    IMUL("imul"), // Signed Multiply
    IDIV("idiv"), // Signed Divide (RDX:RAX / src)
    NEG("neg"), // Negate

    // Logical
    AND("and"), // Bitwise AND
    OR("or"), // Bitwise OR
    XOR("xor"), // Bitwise XOR
    NOT("not"), // Bitwise NOT

    // Shift/Rotate
    SHL("shl"), // Shift Logical Left
    SHR("shr"), // Shift Logical Right
    SAR("sar"), // Shift Arithmetic Right
    // ROL, ROR, RCL, RCR ...

    // Control Flow
    JMP("jmp"), // Unconditional Jump
    CALL("call"), // Call Procedure
    RET("ret"), // Return from Procedure

    // Conditional Jumps (examples)
    JE("je"), JZ("jz"), // Jump if Equal / Jump if Zero
    JNE("jne"), JNZ("jnz"), // Jump if Not Equal / Jump if Not Zero
    JG("jg"), JNLE("jnle"), // Jump if Greater / Jump if Not Less or Equal
    JL("jl"), JNGE("jnge"), // Jump if Less / Jump if Not Greater or Equal
    // ... many more conditional jumps

    // Comparison
    CMP("cmp"), // Compare (sets flags)
    TEST("test"), // Test (sets flags, ANDs operands without storing result)

    // Other
    NOP("nop"),
    CDQ("cdq"), // Convert Double to Quad (sign-extend EAX to EDX:EAX)
    CQO("cqo"); // Convert Quad to Quad (sign-extend RAX to RDX:RAX)

    private final String mnemonic;

    X86Operation(String mnemonic) {
        this.mnemonic = mnemonic;
    }

    public X86Operation fromMnemonic(String mnemonic) {
        for (X86Operation op : values()) {
            if (op.mnemonic.equalsIgnoreCase(mnemonic)) {
                return op;
            }
        }
        throw new IllegalArgumentException("Unknown mnemonic: " + mnemonic);
    }

    private static boolean matches(List<Operand.Kind> kinds, List<List<Operand.Kind>> allowedSignatures) {
        return allowedSignatures.contains(kinds);
    }

    public boolean validateOperands(List<Operand.Kind> kinds) {
        switch (this) {
            case MOV:
                return matches(kinds, List.of(
                        List.of(REGISTER, REGISTER),
                        List.of(REGISTER, IMMEDIATE),
                        List.of(REGISTER, MEMORY),
                        List.of(MEMORY, REGISTER),
                        List.of(MEMORY, IMMEDIATE)));
            default:
                // TODO:
                return false;
        }
    }

    /**
     * @return The assembly mnemonic for the operation (e.g., "mov").
     */
    public String getMnemonic() {
        return mnemonic;
    }

    @Override
    public String toString() {
        return mnemonic;
    }

    // --- Potentially useful properties for register allocation/code generation ---

    /**
     * Indicates if this operation implicitly writes to RFLAGS.
     * This is a simplification; many operations affect specific flags.
     */
    public boolean affectsFlags() {
        return switch (this) {
            case ADD, SUB, INC, DEC, IMUL, IDIV, NEG, AND, OR, XOR, SHL, SHR, SAR, CMP, TEST -> true;
            default -> false;
        };
    }

    /**
     * Indicates if this is a call instruction, which has special implications for
     * register usage
     * (arguments, caller-saved registers).
     */
    public boolean isCall() {
        return this == CALL;
    }

    /**
     * Indicates if this is a return instruction.
     */
    public boolean isReturn() {
        return this == RET;
    }
}
