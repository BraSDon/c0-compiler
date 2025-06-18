package edu.kit.kastel.vads.compiler.backend.regalloc;

import edu.kit.kastel.vads.compiler.ir.node.Node;
import edu.kit.kastel.vads.compiler.ir.node.ProjNode;
import edu.kit.kastel.vads.compiler.ir.node.ReturnNode;
import edu.kit.kastel.vads.compiler.ir.node.ConstIntNode;
import edu.kit.kastel.vads.compiler.ir.node.StartNode;
import edu.kit.kastel.vads.compiler.ir.node.Block;

public final class RegisterRequirement {
    public static boolean needsRegister(Node node) {
        return !(node instanceof ProjNode ||
                node instanceof ReturnNode ||
                node instanceof ConstIntNode ||
                node instanceof StartNode ||
                node instanceof Block);
    }
}
