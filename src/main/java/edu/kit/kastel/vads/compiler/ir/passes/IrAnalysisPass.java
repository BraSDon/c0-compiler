package edu.kit.kastel.vads.compiler.ir.passes;

import edu.kit.kastel.vads.compiler.ir.IrGraph;

public non-sealed interface IrAnalysisPass<R> extends IrPass {

    void analyze(IrGraph graph);

    R getResult();

}
