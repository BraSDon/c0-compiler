
package edu.kit.kastel.vads.compiler.backend.x86;

import edu.kit.kastel.vads.compiler.ir.IrGraph;
import edu.kit.kastel.vads.compiler.ir.node.*; // Wildcard import for IR node types
import edu.kit.kastel.vads.compiler.ir.util.NodeSupport; // For predecessorSkipProj
import edu.kit.kastel.vads.compiler.backend.codegen.CodeGenerator;
import edu.kit.kastel.vads.compiler.backend.x86.ASMBuilder;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class X86CodeGenerator implements CodeGenerator {

    private static final String TEMP_REG_1 = "%eax";
    private static final String TEMP_REG_2 = "%ebx";
    private static final String DIV_REMAINDER_REG = "%edx";

    private static final String RAX = "%rax";
    private static final String RDI = "%rdi";

    private static final String INDENT = "    ";

    public X86Program generateCode(List<IrGraph> programGraphs) {
        ASMBuilder asm = new ASMBuilder(INDENT);

        emitHeader(asm);

        for (IrGraph graph : programGraphs) {
            StackSlotAllocator allocator = new StackSlotAllocator();
            Map<Node, StackSlot> slots = allocator.assignSlots(graph);
            int totalSlotSpace = allocator.getTotalAllocatedSlotSize();
            int alignedStackSpace = (totalSlotSpace + 15) & -16;

            emitFunctionPreamble(asm, "_main", alignedStackSpace);

            Set<Node> visited = new HashSet<>();
            visited.add(graph.endBlock());
            emitCode(graph.endBlock(), visited, asm, slots);

            emitFunctionPostamble(asm, alignedStackSpace);
        }
        return new X86Program(asm.toString());
    }

    private void emitHeader(ASMBuilder asm) {
        asm.formatted("""
                .global main
                .global _main
                .text

                main:
                    call _main
                    movq %s, %s   # Move _main's result (in RAX) to syscall arg0 (RDI)
                    movq $60, %s   # syscall_exit number (0x3C)
                    syscall

                """, RAX, RDI, RAX);
    }

    private void emitFunctionPreamble(ASMBuilder asm, String functionName, int stackSize) {
        asm.ln(functionName + ":");
        asm.ln("pushq %rbp");
        asm.ln("movq %rsp, %rbp");
        if (stackSize > 0) {
            asm.iraw("subq $" + stackSize + ", %rsp").comment("Allocate stack for locals");
        }
    }

    private void emitFunctionPostamble(ASMBuilder asm, int stackSize) {
        if (stackSize > 0) {
            asm.iraw("addq $" + stackSize + ", %rsp").comment("Deallocate stack for locals");
        }
        asm.ln("popq %rbp");
        asm.ln("ret");
    }

    private void emitCode(Node node, Set<Node> visited,
            ASMBuilder asm, Map<Node, StackSlot> slots) {
        for (Node predecessor : node.predecessors()) {
            if (visited.add(predecessor)) {
                emitCode(predecessor, visited, asm, slots);
            }
        }

        switch (node) {
            case ConstIntNode constNode -> {
                StackSlot slot = slots.get(node);
                emitMoveImmToMem(slot.toString(), constNode.value(), asm);
            }
            case AddNode add -> emitBinaryOp(asm, add, "add", slots, "Add");
            case SubNode sub -> emitBinaryOp(asm, sub, "sub", slots, "Sub");
            case MulNode mul -> emitBinaryOp(asm, mul, "imul", slots, "Mul");

            case DivNode div -> emitDiv(asm, div, slots);
            case ModNode mod -> emitMod(asm, mod, slots);
            case ReturnNode ret -> emitReturn(asm, ret, slots);
            case ProjNode _,StartNode _,Block _ -> {
            }
            case Phi _ -> {
                asm.ln("# Phi node encountered - L1 should not have these.");
            }
            default -> {
                asm.formatted("# Unhandled IR Node: %s (%s)", node.getClass().getSimpleName(), node);
            }
        }
    }

    private void emitMoveImmToMem(String memDestAtt, int immVal, ASMBuilder asm) {
        asm.formatted("movl $%d, %s", immVal, memDestAtt);
    }

    private void emitMoveMemToReg(String regDestAtt, String memSrcAtt, ASMBuilder asm) {
        asm.formatted("movl %s, %s", memSrcAtt, regDestAtt);
    }

    private void emitMoveImmToReg(String regDestAtt, int immVal, ASMBuilder asm) {
        asm.formatted("movl $%d, %s", immVal, regDestAtt);
    }

    private void emitMoveRegToMem(String memDestAtt, String regSrcAtt, ASMBuilder asm) {
        asm.formatted("movl %s, %s", regSrcAtt, memDestAtt);
    }

    private void emitDiv(ASMBuilder asm, DivNode div, Map<Node, StackSlot> slots) {
        emitDivOrModOp(asm, div, "div", slots);
    }

    private void emitMod(ASMBuilder asm, ModNode mod, Map<Node, StackSlot> slots) {
        emitDivOrModOp(asm, mod, "mod", slots);
    }

    private void emitReturn(ASMBuilder asm, ReturnNode ret, Map<Node, StackSlot> slots) {
        Node returnValueNode = NodeSupport.predecessorSkipProj(ret, ReturnNode.RESULT);
        StackSlot valueSlot = slots.get(returnValueNode);

        if (valueSlot != null) {
            emitMoveMemToReg(TEMP_REG_1, valueSlot.toString(), asm);
        } else if (returnValueNode instanceof ConstIntNode constVal) {
            emitMoveImmToReg(TEMP_REG_1, constVal.value(), asm);
        } else {
            throw new IllegalStateException(
                    "ReturnNode's operand has no slot and is not a ConstIntNode: " + returnValueNode);
        }
    }

    private void emitDivOrModOp(ASMBuilder asm, BinaryOperationNode divOrMod, String instruction,
            Map<Node, StackSlot> slots) {
        Node left = NodeSupport.predecessorSkipProj(divOrMod, BinaryOperationNode.LEFT);
        Node right = NodeSupport.predecessorSkipProj(divOrMod, BinaryOperationNode.RIGHT);
        StackSlot leftSlot = slots.get(left);
        StackSlot rightSlot = slots.get(right);
        StackSlot resultSlot = slots.get(divOrMod);

        validateSlots(leftSlot, rightSlot, resultSlot, instruction);

        emitMoveMemToReg(TEMP_REG_1, leftSlot.toString(), asm);
        asm.ln("cltd").comment("Sign-extend EAX into EDX:EAX");
        asm.formatted("idivl %s", rightSlot.toString());
        emitMoveRegToMem(resultSlot.toString(), instruction.equals("mod") ? DIV_REMAINDER_REG : TEMP_REG_1, asm);
    }

    private void emitBinaryOp(ASMBuilder asm, BinaryOperationNode opNode, String instruction,
            Map<Node, StackSlot> slots, String opNameComment) {
        Node left = NodeSupport.predecessorSkipProj(opNode, BinaryOperationNode.LEFT);
        Node right = NodeSupport.predecessorSkipProj(opNode, BinaryOperationNode.RIGHT);

        StackSlot leftSlot = slots.get(left);
        StackSlot rightSlot = slots.get(right);
        StackSlot resultSlot = slots.get(opNode);

        validateSlots(leftSlot, rightSlot, resultSlot, opNameComment);

        emitMoveMemToReg(TEMP_REG_1, leftSlot.toString(), asm);

        asm.formatted("%sl %s, %s", instruction, rightSlot.toString(), TEMP_REG_1)
                .comment(opNameComment + ": " + instruction + " " + TEMP_REG_1 + " with RHS from " + rightSlot);

        emitMoveRegToMem(resultSlot.toString(), TEMP_REG_1, asm);
    }

    private void validateSlots(StackSlot leftSlot, StackSlot rightSlot, StackSlot resultSlot, String opNameComment) {
        if (leftSlot == null)
            throw new IllegalStateException(opNameComment + " LHS node has no stack slot");
        if (rightSlot == null)
            throw new IllegalStateException(opNameComment + " RHS node has no stack slot");
        if (resultSlot == null)
            throw new IllegalStateException(opNameComment + " result node has no stack slot");
    }
}
