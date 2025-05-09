package edu.kit.kastel.vads.compiler.backend.x86;

import edu.kit.kastel.vads.compiler.ir.IrGraph;
import edu.kit.kastel.vads.compiler.ir.node.*; // Wildcard import for IR node types
import edu.kit.kastel.vads.compiler.ir.util.NodeSupport; // For predecessorSkipProj
import edu.kit.kastel.vads.compiler.backend.x86.ASMBuilder;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class X86CodeGenerator {

    // Temporary registers for 32-bit operations
    private static final String TEMP_REG_1 = "eax"; // Primary accumulator, first operand, result
    private static final String TEMP_REG_2 = "ebx"; // Secondary operand
    private static final String DIV_REMAINDER_REG = "edx"; // Used by idiv for remainder, and for CDQ

    // 64-bit registers for syscall interface
    private static final String RAX = "rax"; // Syscall number, return value from _main
    private static final String RDI = "rdi"; // First argument to syscall (exit code)

    private static final String INDENT = "    ";

    public String generateCode(List<IrGraph> programGraphs) {
        ASMBuilder asm = new ASMBuilder(INDENT);

        emitHeader(asm);

        for (IrGraph graph : programGraphs) {
            StackSlotAllocator allocator = new StackSlotAllocator();
            Map<Node, StackSlot> slots = allocator.assignSlots(graph);
            int totalSlotSpace = allocator.getTotalAllocatedSlotSize();
            // Align stack space for function frame.
            // RSP should be 16-byte aligned before a call.
            int alignedStackSpace = (totalSlotSpace + 15) & -16;

            emitFunctionPreamble(asm, "_main", alignedStackSpace);

            // Function Body
            Set<Node> visited = new HashSet<>();
            visited.add(graph.endBlock());
            emitCode(graph.endBlock(), visited, asm, slots);

            emitFunctionPostamble(asm, alignedStackSpace);
        }
        return asm.toString();
    }

    private void emitHeader(ASMBuilder asm) {
        // TODO: check
        asm.formatted("""
                .intel_syntax noprefix
                .global main
                .global _main
                .text

                main:
                    call _main
                    mov %s, %s   # Move _main's result (in RAX) to syscall arg0 (RDI)
                    mov %s, 60   # syscall_exit number (0x3C)
                    syscall

                """, RDI, RAX, RAX);
    }

    private void emitFunctionPreamble(ASMBuilder asm, String functionName, int stackSize) {
        asm.ln(functionName + ":");
        asm.ln("push rbp");
        asm.ln("mov rbp, rsp");
        if (stackSize > 0) {
            asm.iraw("sub rsp, " + stackSize).comment("Allocate stack for locals");
        }
    }

    private void emitFunctionPostamble(ASMBuilder asm, int stackSize) {
        if (stackSize > 0) {
            asm.iraw("add rsp, " + stackSize).comment("Deallocate stack for locals");
        }
        asm.ln("pop rbp");
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
                StackSlot slot = slots.get(constNode);
                asm.formatted("mov %s, %s", slot.toDwordPtrString(), constNode.value())
                        .comment("Const " + constNode.value() + " -> " + slot);
            }
            case AddNode add -> emitBinaryOp(asm, add, "add", slots, "Add");
            case SubNode sub -> emitBinaryOp(asm, sub, "sub", slots, "Sub");
            case MulNode mul -> emitBinaryOp(asm, mul, "imul", slots, "Mul"); // Signed multiplication

            case DivNode div -> {
                // idiv r/m32: Divides EDX:EAX by r/m32. Quotient in EAX, Remainder in EDX.
                Node left = NodeSupport.predecessorSkipProj(div, BinaryOperationNode.LEFT);
                Node right = NodeSupport.predecessorSkipProj(div, BinaryOperationNode.RIGHT);
                StackSlot leftSlot = slots.get(left);
                StackSlot rightSlot = slots.get(right);
                StackSlot resultSlot = slots.get(div);

                asm.formatted("mov %s, %s", TEMP_REG_1, leftSlot.toDwordPtrString())
                        .comment("Div: Load dividend (LHS) into " + TEMP_REG_1);
                asm.ln("cdq").comment("Sign-extend EAX into EDX:EAX");
                asm.formatted("idiv %s", rightSlot.toDwordPtrString())
                        .comment("Divide by divisor (RHS)");
                asm.formatted("mov %s, %s", resultSlot.toDwordPtrString(), TEMP_REG_1)
                        .comment("Store quotient (in EAX) into " + resultSlot);
            }
            case ModNode mod -> {
                Node left = NodeSupport.predecessorSkipProj(mod, BinaryOperationNode.LEFT);
                Node right = NodeSupport.predecessorSkipProj(mod, BinaryOperationNode.RIGHT);
                StackSlot leftSlot = slots.get(left);
                StackSlot rightSlot = slots.get(right);
                StackSlot resultSlot = slots.get(mod);

                asm.formatted("mov %s, %s", TEMP_REG_1, leftSlot.toDwordPtrString())
                        .comment("Mod: Load dividend (LHS) into " + TEMP_REG_1);

                asm.ln("cdq").comment("Sign-extend EAX into EDX:EAX");
                asm.formatted("idiv %s", rightSlot.toDwordPtrString())
                        .comment("Divide by divisor (RHS)");
                asm.formatted("mov %s, %s", resultSlot.toDwordPtrString(), DIV_REMAINDER_REG)
                        .comment("Store remainder (in EDX) into " + resultSlot);
            }
            case ReturnNode ret -> {
                Node returnValueNode = NodeSupport.predecessorSkipProj(ret, ReturnNode.RESULT);
                StackSlot valueSlot = slots.get(returnValueNode);

                if (valueSlot != null) {
                    asm.formatted("mov %s, %s", TEMP_REG_1, valueSlot.toDwordPtrString())
                            .comment("Load return value from " + valueSlot + " into " + TEMP_REG_1);
                } else if (returnValueNode instanceof ConstIntNode constVal) {
                    // This case handles `return <literal>;` if constants are not put in slots by
                    // default.
                    // Current `needsStackSlot` and `StackSlotAllocator` puts all ConstIntNodes in
                    // slots.
                    // So this branch might be defensive/unreachable with current allocator logic.
                    asm.formatted("mov %s, %s", TEMP_REG_1, constVal.value())
                            .comment("Load immediate return value into " + TEMP_REG_1);
                } else {
                    throw new IllegalStateException(
                            "ReturnNode's operand has no slot and is not a ConstIntNode: " + returnValueNode);
                }
                // The actual 'ret' instruction is in the function postamble. EAX holds the
                // return value.
            }
            case ProjNode _,StartNode _,Block _ -> {
                /* Structural nodes, no direct code for spill-everything */ }
            case Phi _ -> {
                // L1 is not expected to generate Phi nodes.
                asm.ln("# Phi node encountered - L1 should not have these.");
            }
            default -> {
                asm.formatted("# Unhandled IR Node: %s (%s)", node.getClass().getSimpleName(), node);
            }
        }
    }

    private void emitBinaryOp(ASMBuilder asm, BinaryOperationNode opNode, String instruction,
            Map<Node, StackSlot> stackSlotsMap, String opNameComment) {
        Node left = NodeSupport.predecessorSkipProj(opNode, BinaryOperationNode.LEFT);
        Node right = NodeSupport.predecessorSkipProj(opNode, BinaryOperationNode.RIGHT);

        StackSlot leftSlot = stackSlotsMap.get(left);
        StackSlot rightSlot = stackSlotsMap.get(right);
        StackSlot resultSlot = stackSlotsMap.get(opNode);

        // Sanity checks
        if (leftSlot == null)
            throw new IllegalStateException(opNameComment + " LHS node has no stack slot: " + left);
        if (rightSlot == null)
            throw new IllegalStateException(opNameComment + " RHS node has no stack slot: " + right);
        if (resultSlot == null)
            throw new IllegalStateException(opNameComment + " result node has no stack slot: " + opNode);

        // Most x86 arithmetic instructions can use one register and one memory operand.
        // Operation: reg = reg op mem (e.g., add eax, [rbp - offset_right])
        asm.formatted("mov %s, %s", TEMP_REG_1, leftSlot.toDwordPtrString())
                .comment(opNameComment + ": Load LHS from " + leftSlot + " into " + TEMP_REG_1);

        asm.formatted("%s %s, %s", instruction, TEMP_REG_1, rightSlot.toDwordPtrString())
                .comment(opNameComment + ": " + instruction + " " + TEMP_REG_1 + " with RHS from " + rightSlot);

        asm.formatted("mov %s, %s", resultSlot.toDwordPtrString(), TEMP_REG_1)
                .comment(opNameComment + ": Store result from " + TEMP_REG_1 + " to " + resultSlot);
    }
}
