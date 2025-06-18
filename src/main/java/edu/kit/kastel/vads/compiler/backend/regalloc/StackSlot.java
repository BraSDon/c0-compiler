package edu.kit.kastel.vads.compiler.backend.regalloc;

public record StackSlot(int positiveOffset) implements Location {
    public static final int SLOT_SIZE = 8;

    public StackSlot {
        if (positiveOffset <= 0 || positiveOffset % SLOT_SIZE != 0) {
            throw new IllegalArgumentException(
                    "Slot offset must be positive and a multiple of " + SLOT_SIZE + ". Got: " + positiveOffset);
        }
    }

    @Override
    public String toString() {
        // TODO: adjust for 64-bit architecture
        return "dword ptr [rbp - " + positiveOffset + "]";
    }
}
