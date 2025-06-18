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
    R15("r15", false, "Callee-saved"),

    // General Purpose Registers (32-bit counterparts)
    EAX("eax", true, "32-bit Accumulator"),
    EBX("ebx", false, "32-bit Base"),
    ECX("ecx", true, "32-bit Counter"),
    EDX("edx", true, "32-bit Data"),
    ESI("esi", true, "32-bit Source Index"),
    EDI("edi", true, "32-bit Destination Index"),
    EBP("ebp", false, "32-bit Base Pointer"),
    ESP("esp", false, "32-bit Stack Pointer"),
    R8D("r8d", true, "32-bit R8"),
    R9D("r9d", true, "32-bit R9"),
    R10D("r10d", true, "32-bit R10"),
    R11D("r11d", true, "32-bit R11"),
    R12D("r12d", false, "32-bit R12"),
    R13D("r13d", false, "32-bit R13"),
    R14D("r14d", false, "32-bit R14"),
    R15D("r15d", false, "32-bit R15");

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

    public X86Register get32BitCounterpart() {
        return switch (this) {
            case RAX -> EAX;
            case RBX -> EBX;
            case RCX -> ECX;
            case RDX -> EDX;
            case RSI -> ESI;
            case RDI -> EDI;
            case RBP -> EBP;
            case RSP -> ESP;
            case R8 -> R8D;
            case R9 -> R9D;
            case R10 -> R10D;
            case R11 -> R11D;
            case R12 -> R12D;
            case R13 -> R13D;
            case R14 -> R14D;
            case R15 -> R15D;
            // If already 32-bit, return itself
            case EAX, EBX, ECX, EDX, ESI, EDI, EBP, ESP, R8D, R9D, R10D, R11D, R12D, R13D, R14D, R15D -> this;
            default -> throw new IllegalStateException("No 32-bit counterpart for " + this);
        };
    }

    public X86Register get64BitCounterpart() {
        return switch (this) {
            case EAX -> RAX;
            case EBX -> RBX;
            case ECX -> RCX;
            case EDX -> RDX;
            case ESI -> RSI;
            case EDI -> RDI;
            case EBP -> RBP;
            case ESP -> RSP;
            case R8D -> R8;
            case R9D -> R9;
            case R10D -> R10;
            case R11D -> R11;
            case R12D -> R12;
            case R13D -> R13;
            case R14D -> R14;
            case R15D -> R15;
            // If already 64-bit, return itself
            case RAX, RBX, RCX, RDX, RSI, RDI, RBP, RSP, R8, R9, R10, R11, R12, R13, R14, R15 -> this;
            default -> throw new IllegalStateException("No 64-bit counterpart for " + this);
        };
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
    public static final X86Register SCRATCH_64 = R11;
    public static final X86Register SCRATCH_32 = R11D; // 32-bit scratch register

    private static final Set<X86Register> ALL_GPRS = Collections.unmodifiableSet(EnumSet.allOf(X86Register.class));
    private static final Set<X86Register> ALL_32_BIT_GPRS = Collections.unmodifiableSet(
            EnumSet.of(EAX, EBX, ECX, EDX, ESI, EDI, EBP, ESP, R8D, R9D, R10D, R11D, R12D, R13D, R14D, R15D));
    private static final Set<X86Register> ALL_64_BIT_GPRS = Collections.unmodifiableSet(
            EnumSet.of(RAX, RBX, RCX, RDX, RSI, RDI, RBP, RSP, R8, R9, R10, R11, R12, R13, R14, R15));

    private static final Set<X86Register> NON_ALLOCATABLE_GPRS = Collections.unmodifiableSet(
            EnumSet.of(RSP, RBP, SCRATCH_64, RAX, RBX, RDX));

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
        EnumSet<X86Register> nonAllocatable = EnumSet.of(RSP, RBP, SCRATCH_64, RAX, RBX, RDX);
        allocatable.removeAll(nonAllocatable);
        // Remove 32-bit counterparts of non-allocatable registers
        for (X86Register reg : nonAllocatable) {
            allocatable.remove(reg.get32BitCounterpart());
        }
        return Collections.unmodifiableSet(allocatable);
    }

    public static Set<X86Register> allocatable32BitGPRs() {
        EnumSet<X86Register> allocatable32Bit = EnumSet.copyOf(ALL_32_BIT_GPRS);
        allocatable32Bit.removeAll(NON_ALLOCATABLE_GPRS.stream()
                .map(X86Register::get32BitCounterpart)
                .collect(Collectors.toSet()));
        return Collections.unmodifiableSet(allocatable32Bit);

    }

}
