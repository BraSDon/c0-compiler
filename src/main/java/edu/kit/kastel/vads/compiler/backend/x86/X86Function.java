package edu.kit.kastel.vads.compiler.backend.x86;

import java.util.ArrayList;
import java.util.List;

public class X86Function {
    private final String label;
    private final List<X86Instruction> instructions = new ArrayList<>();
    private String preamble;
    private String postamble;

    public X86Function(String label, String preamble, String postamble) {
        this.label = label;
        this.preamble = preamble;
        this.postamble = postamble;
    }

    public void addInstruction(X86Instruction instr) {
        instructions.add(instr);
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        // TODO: actual solution instead of hotfix.
        sb.append("_").append(label).append(":\n");
        sb.append(preamble).append('\n');
        sb.append("    ").append("# Preamble finished").append('\n');
        instructions.forEach(i -> sb.append("    ").append(i).append('\n'));
        sb.append("    ").append("# Instructions finished\n");
        sb.append(postamble).append('\n');
        return sb.toString();
    }
}
