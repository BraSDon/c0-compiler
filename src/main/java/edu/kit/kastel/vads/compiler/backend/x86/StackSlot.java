package edu.kit.kastel.vads.compiler.backend.x86;

public record StackSlot(int positiveOffset) { // e.g., 4, 8, 12 (bytes from rbp)
    public StackSlot {
        if (positiveOffset <= 0 || positiveOffset % 4 != 0) {
            throw new IllegalArgumentException(
                    "Slot offset must be positive and a multiple of 4. Got: " + positiveOffset);
        }
    }

    @Override
    public String toString() {
        return String.format("-%d(%%rbp)", positiveOffset);
    }
}
