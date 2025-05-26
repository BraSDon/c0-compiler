package edu.kit.kastel.vads.compiler.backend.x86;

import edu.kit.kastel.vads.compiler.backend.regalloc.Register;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Represents x86-64 general-purpose registers.
 * Metadata includes typical usage and calling convention properties (System V
 * AMD64 ABI).
 */
public enum X86Register implements Register {
    // General Purpose Registers (64-bit)
    RAX("rax", true, "Return value / Accumulator"),
    RBX("rbx", false, "Callee-saved / Base"),
    RCX("rcx", true, "Counter / 4th argument"),
    RDX("rdx", true, "Data / 3rd argument / Return value extension"),
    RSI("rsi", true, "Source Index / 2nd argument"),
    RDI("rdi", true, "Destination Index / 1st argument"),
    RBP("rbp", false, "Base Pointer (Callee-saved)"),
    RSP("rsp", false, "Stack Pointer (Special, Callee-saved but managed explicitly)"), // Callee-saved, but special
    R8("r8", true, "5th argument"),
    R9("r9", true, "6th argument"),
    R10("r10", true, "Temporary / Scratch"),
    R11("r11", true, "Temporary / Scratch"),
    R12("r12", false, "Callee-saved"),
    R13("r13", false, "Callee-saved"),
    R14("r14", false, "Callee-saved"),
    R15("r15", false, "Callee-saved");

    // RIP (Instruction Pointer) and RFLAGS (Flags) are special and not typically
    // GPRs for allocation.

    private final String mnemonic;
    private final boolean callerSaved;
    private final String description;

    X86Register(String mnemonic, boolean callerSaved, String description) {
        this.mnemonic = mnemonic;
        this.callerSaved = callerSaved;
        this.description = description;
    }

    /**
     * @return The assembly mnemonic for the register (e.g., "rax").
     */
    public String getMnemonic() {
        return mnemonic;
    }

    /**
     * @return True if the register is caller-saved under the System V AMD64 ABI.
     */
    public boolean isCallerSaved() {
        return callerSaved;
    }

    /**
     * @return True if the register is callee-saved under the System V AMD64 ABI.
     */
    public boolean isCalleeSaved() {
        return !callerSaved; // RSP is special but generally treated as callee-saved
    }

    /**
     * @return A brief description of the register's typical use.
     */
    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return mnemonic;
    }

    // --- Convenience Sets for Register Allocation ---

    private static final Set<X86Register> ALL_GPRS = Collections.unmodifiableSet(EnumSet.allOf(X86Register.class));

    private static final Set<X86Register> CALLER_SAVED_GPRS = Collections.unmodifiableSet(
            Arrays.stream(values())
                    .filter(X86Register::isCallerSaved)
                    .collect(Collectors.toCollection(() -> EnumSet.noneOf(X86Register.class))));

    private static final Set<X86Register> CALLEE_SAVED_GPRS = Collections.unmodifiableSet(
            Arrays.stream(values())
                    .filter(X86Register::isCalleeSaved)
                    .collect(Collectors.toCollection(() -> EnumSet.noneOf(X86Register.class))));

    /**
     * @return An unmodifiable set of all general-purpose registers defined in this
     *         enum.
     */
    public static Set<X86Register> allGPRs() {
        return ALL_GPRS;
    }

    /**
     * @return An unmodifiable set of caller-saved general-purpose registers.
     */
    public static Set<X86Register> callerSavedGPRs() {
        return CALLER_SAVED_GPRS;
    }

    /**
     * @return An unmodifiable set of callee-saved general-purpose registers.
     */
    public static Set<X86Register> calleeSavedGPRs() {
        return CALLEE_SAVED_GPRS;
    }

    /**
     * @return An unmodifiable set of GPRs typically available for allocation
     *         (excluding RSP and RBP if they are used for frame/stack pointers).
     */
    public static Set<X86Register> allocatableGPRs() {
        EnumSet<X86Register> allocatable = EnumSet.allOf(X86Register.class);
        allocatable.remove(RSP);
        allocatable.remove(RBP);
        allocatable.remove(SCRATCH); // Scratch register is reserved for load from stack
        return Collections.unmodifiableSet(allocatable);
    }

    public static final X86Register SCRATCH = R11;
}
