package edu.kit.kastel.vads.compiler.backend.codegen;

import java.io.IOException;
import java.nio.file.Path;

public interface AssemblyProgram {

    String INDENT = "    ";

    /**
     * Compiles the assembly program to an executable.
     * If the program was not written to a file yet, it will be written to the same
     * base path as the executable.
     * 
     * @param executablePath the path where the executable will be created
     */
    void compile(Path executablePath) throws IOException;

    void writeToFile(Path filePath) throws IOException;

    String toString();
}
