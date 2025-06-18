
package edu.kit.kastel.vads.compiler.backend.x86;

import edu.kit.kastel.vads.compiler.ir.IrGraph;
import edu.kit.kastel.vads.compiler.ir.node.*; // Wildcard import for IR node types
import edu.kit.kastel.vads.compiler.ir.util.NodeSupport; // For predecessorSkipProj
import edu.kit.kastel.vads.compiler.backend.codegen.*;
import edu.kit.kastel.vads.compiler.backend.regalloc.Location;
import edu.kit.kastel.vads.compiler.backend.regalloc.StackSlot;
import edu.kit.kastel.vads.compiler.backend.x86.ASMBuilder;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class X86CodeGenerator implements CodeGenerator {

    private static final String INDENT = "    ";

    private X86Program program;

    public X86Program generateCode(List<IrGraph> programGraphs) {
        program = new X86Program();
        ASMBuilder asm = new ASMBuilder(INDENT);

        program.addHeader(emitHeader());

        for (IrGraph graph : programGraphs) {
            Set<Node> visited = new HashSet<>();
            visited.add(graph.endBlock());

            var allocator = new X86RegisterAllocator();
            Map<Node, Location> locations = allocator.allocateRegisters(graph);
            int totalStackSize = allocator.getTotalAllocatedStackSize();
            int alignedStackSpace = (totalStackSize + 15) & -16;

            // TODO: check if name is correct for main function
            String preamble = getFnPreamble(alignedStackSpace);
            String postamble = getFnPostamble(alignedStackSpace);
            program.startFunction(graph.name(), preamble, postamble);

            emitCode(graph.endBlock(), visited, asm, locations);
        }
        return program;
    }

    private String emitHeader() {
        return String.format("""
                .intel_syntax noprefix
                .global main
                .global _main
                .text

                main:
                    call _main
                    mov %s, %s
                    mov %s, 60
                    syscall

                """, X86Register.RDI, X86Register.RAX, X86Register.RAX);
    }

    private String getFnPreamble(int stackSize) {
        ASMBuilder asm = new ASMBuilder(INDENT);
        asm.ln("push rbp");
        asm.ln("mov rbp, rsp");
        if (stackSize > 0) {
            asm.iraw("sub rsp, " + stackSize).comment("Allocate stack for locals");
        }
        return asm.toString();
    }

    private String getFnPostamble(int stackSize) {
        ASMBuilder asm = new ASMBuilder(INDENT);
        if (stackSize > 0) {
            asm.iraw("add rsp, " + stackSize).comment("Deallocate stack for locals");
        }
        asm.ln("pop rbp");
        asm.ln("ret");
        return asm.toString();
    }

    private void emitCode(Node node, Set<Node> visited,
            ASMBuilder asm, Map<Node, Location> locations) {
        for (Node predecessor : node.predecessors()) {
            if (visited.add(predecessor)) {
                emitCode(predecessor, visited, asm, locations);
            }
        }

        switch (node) {
            case AddNode add -> emitBinaryOp(add, X86Operation.ADD, locations);
            case SubNode sub -> emitBinaryOp(sub, X86Operation.SUB, locations);
            case MulNode mul -> emitBinaryOp(mul, X86Operation.IMUL, locations);

            case DivNode div -> emitDiv(div, locations);
            case ModNode mod -> emitMod(mod, locations);
            case ReturnNode ret -> emitReturn(ret, locations);
            case ProjNode _,StartNode _,Block _,ConstIntNode _ -> {
            }
            case Phi _ -> {
                asm.ln("# Phi node encountered - L1 should not have these.");
            }
            default -> {
                asm.formatted("# Unhandled IR Node: %s (%s)", node.getClass().getSimpleName(), node);
            }
        }
    }

    record LocationPair(Location dest, Location src) {
    }

    private void emitMove(Location dest, Location src) {
        switch (new LocationPair(dest, src)) {
            case LocationPair(X86Register d, X86Register s) -> emitMoveRegToReg(d, s);
            case LocationPair(X86Register d, StackSlot s) -> emitMoveMemToReg(d, s);
            case LocationPair(StackSlot d, X86Register s) -> emitMoveRegToMem(d, s);
            default -> {
                throw new IllegalStateException("Unhandled move pair: " + dest + " -> " + src);
            }
        }
    }

    private void emitMoveImmToMem(StackSlot slot, int immVal) {
        Operand imm = new ImmediateOperand(immVal);
        Operand mem = new MemoryOperand(slot);
        program.addInstruction(new X86Instruction(X86Operation.MOV, mem, imm));
    }

    private void emitMoveImmToReg(X86Register register, int immVal) {
        Operand imm = new ImmediateOperand(immVal);
        Operand reg = new RegisterOperand(register);
        program.addInstruction(new X86Instruction(X86Operation.MOV, reg, imm));
    }

    private void emitMoveMemToReg(X86Register register, StackSlot slot) {
        Operand mem = new MemoryOperand(slot);
        Operand reg = new RegisterOperand(register);
        program.addInstruction(new X86Instruction(X86Operation.MOV, reg, mem));
    }

    private void emitMoveRegToMem(StackSlot slot, X86Register register) {
        Operand reg = new RegisterOperand(register);
        Operand mem = new MemoryOperand(slot);
        program.addInstruction(new X86Instruction(X86Operation.MOV, mem, reg));
    }

    private void emitMoveRegToReg(X86Register dest, X86Register src) {
        // No need to move if both registers are the same
        if (dest.equals(src)) {
            return;
        }
        Operand srcReg = new RegisterOperand(src);
        Operand destReg = new RegisterOperand(dest);
        program.addInstruction(new X86Instruction(X86Operation.MOV, destReg, srcReg));
    }

    private void emitDiv(DivNode div, Map<Node, Location> locations) {
        emitDivOrModOp(div, "div", locations);
    }

    private void emitMod(ModNode mod, Map<Node, Location> locations) {
        emitDivOrModOp(mod, "mod", locations);
    }

    private void emitReturn(ReturnNode ret, Map<Node, Location> locations) {
        Node returnValueNode = NodeSupport.predecessorSkipProj(ret, ReturnNode.RESULT);
        if (returnValueNode instanceof ConstIntNode constNode) {
            emitMoveImmToReg(X86Register.RAX, constNode.value());
            return;
        }

        Location loc = locations.get(returnValueNode);

        switch (loc) {
            case StackSlot s -> emitMoveMemToReg(X86Register.RAX, s);
            case X86Register r -> emitMoveRegToReg(X86Register.RAX, r);
            default -> {
                if (returnValueNode instanceof ConstIntNode constNode) {
                    emitMoveImmToReg(X86Register.RAX, constNode.value());
                }
            }
        }
    }

    private void emitDivOrModOp(BinaryOperationNode divOrMod, String op,
            Map<Node, Location> locations) {
        Node left = NodeSupport.predecessorSkipProj(divOrMod, BinaryOperationNode.LEFT);
        Node right = NodeSupport.predecessorSkipProj(divOrMod, BinaryOperationNode.RIGHT);
        Location leftLoc = locations.get(left);
        Location rightLoc = locations.get(right);
        Location resultLoc = locations.get(divOrMod);

        Operand leftOperand = nodeToOperand(left, locations);
        Operand rightOperand = nodeToOperand(right, locations);

        if (leftOperand instanceof ImmediateOperand leftImm) {
            emitMoveImmToReg(X86Register.RAX, leftImm.value());
        } else {
            emitMove(X86Register.RAX, leftLoc);
        }

        if (rightOperand instanceof ImmediateOperand rightImm) {
            rightOperand = new RegisterOperand(X86Register.SCRATCH);
            emitMoveImmToReg(X86Register.SCRATCH, rightImm.value());
        }

        program.addInstruction(new X86Instruction(X86Operation.CQO));

        program.addInstruction(new X86Instruction(
                X86Operation.IDIV,
                rightOperand));

        emitMove(resultLoc, op.equals("mod") ? X86Register.RDX : X86Register.RAX);
    }

    record OperandPair(Operand left, Operand right) {
    }

    // TODO: Fix bug in translating inputs/signed_mul
    private void emitBinaryOp(BinaryOperationNode opNode, X86Operation op, Map<Node, Location> locations) {
        Node left = NodeSupport.predecessorSkipProj(opNode, BinaryOperationNode.LEFT);
        Node right = NodeSupport.predecessorSkipProj(opNode, BinaryOperationNode.RIGHT);

        Operand leftOperand = nodeToOperand(left, locations);
        Operand rightOperand = nodeToOperand(right, locations);

        Location leftLoc = locations.get(left);
        Location resultLoc = locations.get(opNode);

        switch (new OperandPair(leftOperand, rightOperand)) {
            case OperandPair(ImmediateOperand leftImm, _) -> {
                emitMoveImmToReg(X86Register.SCRATCH, leftImm.value());
                program.addInstruction(new X86Instruction(
                        op,
                        new RegisterOperand(X86Register.SCRATCH),
                        rightOperand));
                emitMove(resultLoc, X86Register.SCRATCH);
            }
            case OperandPair(MemoryOperand leftMem, MemoryOperand _) -> {
                emitMoveMemToReg(X86Register.SCRATCH, leftMem.stackSlot());
                program.addInstruction(new X86Instruction(
                        op,
                        new RegisterOperand(X86Register.SCRATCH),
                        rightOperand));
                emitMove(resultLoc, X86Register.SCRATCH);
            }
            default -> {
                program.addInstruction(new X86Instruction(
                        op,
                        leftOperand,
                        rightOperand));
                emitMove(resultLoc, leftLoc);
            }
        }
    }

    private static Operand nodeToOperand(Node node, Map<Node, Location> locations) {
        if (node instanceof ConstIntNode constNode) {
            return new ImmediateOperand(constNode.value());
        }
        Location loc = locations.get(node);
        if (loc == null) {
            throw new IllegalStateException("No location for node: " + node);
        }
        return toOperand(loc);
    }

    private static Operand toOperand(Location loc) {
        switch (loc) {
            case X86Register reg -> {
                return new RegisterOperand(reg);
            }
            case StackSlot slot -> {
                return new MemoryOperand(slot);
            }
            default -> throw new IllegalStateException("Unhandled location type: " + loc.getClass().getSimpleName());
        }
    }
}
