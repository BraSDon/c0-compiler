package edu.kit.kastel.vads.compiler;

import edu.kit.kastel.vads.compiler.backend.x86.X86CodeGenerator;
import edu.kit.kastel.vads.compiler.backend.x86.X86Program;
import edu.kit.kastel.vads.compiler.ir.IrGraph;
import edu.kit.kastel.vads.compiler.ir.SsaTranslation;
import edu.kit.kastel.vads.compiler.ir.optimize.LocalValueNumbering;
import edu.kit.kastel.vads.compiler.ir.util.YCompPrinter;
import edu.kit.kastel.vads.compiler.ir.util.LivenessPrinter;
import edu.kit.kastel.vads.compiler.ir.passes.analysis.LivenessAnalysis;
import edu.kit.kastel.vads.compiler.lexer.Lexer;
import edu.kit.kastel.vads.compiler.parser.ParseException;
import edu.kit.kastel.vads.compiler.parser.Parser;
import edu.kit.kastel.vads.compiler.parser.Printer;
import edu.kit.kastel.vads.compiler.parser.TokenSource;
import edu.kit.kastel.vads.compiler.parser.ast.ProgramTree;
import edu.kit.kastel.vads.compiler.semantic.SemanticAnalysis;
import edu.kit.kastel.vads.compiler.semantic.SemanticException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Main {
    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err.println("Invalid arguments: Expected one input file and one output file");
            System.exit(3);
        }

        Path input = Path.of(args[0]);
        Path output = Path.of(args[1]);

        ProgramTree program = lexAndParse(input);

        System.out.println(Printer.print(program));

        try {
            new SemanticAnalysis(program).analyze();
        } catch (SemanticException e) {
            e.printStackTrace();
            System.exit(7);
        }

        // Each top-level tree is a function, which will be translated into its own
        // IrGraph.
        List<IrGraph> graphs = program.topLevelTrees().stream()
                .map(f -> new SsaTranslation(f, new LocalValueNumbering()).translate())
                .toList();

        var livenessResults = graphs.stream()
                .map(graph -> {
                    LivenessAnalysis livenessAnalysis = new LivenessAnalysis();
                    livenessAnalysis.analyze(graph);
                    return livenessAnalysis.getResult();
                })
                .toList();

        LivenessPrinter.printLiveness(graphs.get(0), livenessResults.get(0));

        if ("vcg".equals(System.getenv("DUMP_GRAPHS")) || "vcg".equals(System.getProperty("dumpGraphs"))) {
            Path tmp = output.toAbsolutePath().resolveSibling("graphs");
            Files.createDirectories(tmp);
            String inputFileName = output.getFileName().toString().replaceFirst("[.][^.]+$", "");
            for (IrGraph graph : graphs) {
                dumpGraph(graph, tmp, inputFileName);
            }
        }

        X86Program x86Program = new X86CodeGenerator().generateCode(graphs);
        x86Program.compile(output);
    }

    private static ProgramTree lexAndParse(Path input) throws IOException {
        try {
            Lexer lexer = Lexer.forString(Files.readString(input));
            TokenSource tokenSource = new TokenSource(lexer);
            Parser parser = new Parser(tokenSource);
            return parser.parseProgram();
        } catch (ParseException e) {
            e.printStackTrace();
            System.exit(42);
            throw new AssertionError("unreachable");
        }
    }

    private static void dumpGraph(IrGraph graph, Path path, String key) throws IOException {
        Files.writeString(
                path.resolve(graph.name() + "-" + key + ".vcg"),
                YCompPrinter.print(graph));
    }
}
