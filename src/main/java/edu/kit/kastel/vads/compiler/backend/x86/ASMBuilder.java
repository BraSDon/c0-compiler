package edu.kit.kastel.vads.compiler.backend.x86;

public class ASMBuilder {
    private final StringBuilder sb = new StringBuilder();
    private final String indent;

    public ASMBuilder(String indent) {
        this.indent = indent;
    }

    public ASMBuilder ln(String line) {
        sb.append(indent).append(line).append('\n');
        return this;
    }

    public ASMBuilder iraw(String line) {
        sb.append(indent).append(line);
        return this;
    }

    public ASMBuilder raw(String line) {
        sb.append(line);
        return this;
    }

    public ASMBuilder rawln(String line) {
        sb.append(line).append('\n');
        return this;
    }

    public ASMBuilder formatted(String format, Object... args) {
        sb.append(indent).append(String.format(format, args)).append('\n');
        return this;
    }

    public ASMBuilder comment(String comment) {
        sb.append("  # ").append(comment).append('\n');
        return this;
    }

    @Override
    public String toString() {
        return sb.toString();
    }
}
