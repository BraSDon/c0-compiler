package edu.kit.kastel.vads.compiler.ir.passes;

import edu.kit.kastel.vads.compiler.ir.IrGraph;

public non-sealed interface IrTransformPass extends IrPass {

    IrGraph transform(IrGraph graph);

}
