package edu.kit.kastel.vads.compiler.backend.regalloc;

public sealed interface Location permits Register, StackSlot {
}
