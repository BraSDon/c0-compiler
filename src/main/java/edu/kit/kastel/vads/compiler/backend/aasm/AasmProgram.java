package edu.kit.kastel.vads.compiler.backend.aasm;

import edu.kit.kastel.vads.compiler.backend.codegen.AssemblyProgram;

import java.nio.file.Path;
import java.util.List;

public record AasmProgram(List<AasmInstruction> instructions) implements AssemblyProgram {

    @Override
    public void compile(Path executablePath) {
        // Aasm doesnt need to compile anything.
    }

    @Override
    public void writeToFile(Path filePath) {
        // Aasm doesn't need to write anything to a file.
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (AasmInstruction instruction : instructions) {
            sb.append(instruction.toAssembly()).append("\n");
        }
        return sb.toString();
    }

}
