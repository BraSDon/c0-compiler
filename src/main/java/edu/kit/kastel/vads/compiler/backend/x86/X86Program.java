package edu.kit.kastel.vads.compiler.backend.x86;

import edu.kit.kastel.vads.compiler.backend.codegen.AssemblyProgram;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.Optional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;

public class X86Program implements AssemblyProgram {

    private final List<String> headers = new ArrayList<>();
    private final List<X86Function> functions = new ArrayList<>();
    private Optional<X86Function> currFn = Optional.empty();

    public void addHeader(String header) {
        headers.add(header);
    }

    public void startFunction(String label, String preamble, String postamble) {
        currFn = Optional.of(new X86Function(label, preamble, postamble));
        functions.add(currFn.get());
    }

    public void addInstruction(X86Instruction instr) {
        currFn.ifPresent(fn -> fn.addInstruction(instr));
    }

    public String currFnLabel() {
        return currFn.map(X86Function::getLabel).orElse("");
    }

    @Override
    public void compile(Path executablePath) throws IOException {
        Path tempAsmFile = executablePath.resolveSibling(executablePath.getFileName() + ".s");
        writeToFile(tempAsmFile);

        int exitCode = runGccWithCleanup(tempAsmFile, executablePath, 10);
        System.out.println("GCC exit code: " + exitCode);
    }

    @Override
    public void writeToFile(Path filePath) throws IOException {
        String fileName = filePath.toString();
        if (!fileName.endsWith(".s")) {
            filePath = filePath.resolveSibling(fileName + ".s");
        }
        Files.writeString(filePath, toString());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        headers.forEach(header -> sb.append(header).append(System.lineSeparator()));
        functions.forEach(fn -> sb.append(fn.toString()).append(System.lineSeparator()));
        return sb.toString();
    }

    private static int runGccWithCleanup(Path asmPath, Path exePath, int timeoutSec) {
        try {
            return invokeGcc(asmPath, exePath, timeoutSec);
        } catch (IOException | InterruptedException e) {
            System.err.println("Error invoking GCC: " + e.getMessage());
            e.printStackTrace();
            System.exit(42);
            return -1; // unreachable but required
        }
    }

    public static int invokeGcc(Path asmPath, Path exePath, int timeoutSec)
            throws IOException, InterruptedException {

        List<String> command = List.of(
                "gcc",
                asmPath.toAbsolutePath().toString(),
                "-o",
                exePath.toAbsolutePath().toString());

        System.out.println("Executing GCC command: " + String.join(" ", command));

        Process process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append(System.lineSeparator());
            }
        }

        boolean finished = process.waitFor(timeoutSec, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            System.err.println("GCC process timed out after " + timeoutSec + " seconds.");
            System.err.println("GCC Output (partial):\n" + output);
            return -1;
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            System.err.println("GCC failed with exit code: " + exitCode);
            System.err.println("GCC Output:\n" + output);
        } else {
            System.out.println("GCC compilation successful.");
        }

        return exitCode;
    }
}
