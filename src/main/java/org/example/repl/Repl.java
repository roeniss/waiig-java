package org.example.repl;

import org.example.ast.Program;
import org.example.evaluator.Evaluator;
import org.example.evaluator.MacroExpansion;
import org.example.lexer.Lexer;
import org.example.object.Builtins;
import org.example.object.Environment;
import org.example.object.MonkeyObject;
import org.example.parser.Parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.List;

public class Repl {

    private static final String PROMPT = ">> ";

    private static final String MONKEY_FACE = """
                        __,__
               .--.  .-"     "-.  .--.
              / .. \\/  .-. .-.  \\/ .. \\
             | |  '|  /   Y   \\  |'  | |
             | \\   \\  \\ 0 | 0 /  /   / |
              \\ '- ,\\.-\"\"\"\"\"\"\"-./, -' /
               ''-' /_   ^ ^   _\\ '-''
                   |  \\._   _./  |
                   \\   \\ '~' /   /
                    '._ '-=-' _.'
                       '-----'
            """;

    public static void start(InputStream in, PrintStream out) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        // one environment for the whole session, so a let on one line is visible on the next
        Environment env = new Environment();
        // macros live apart from ordinary values: they are defined and expanded before evaluation starts
        Environment macroEnv = new Environment();

        // puts writes to the same stream as the REPL; the previous target is restored when the session ends
        PrintStream previousOutput = Builtins.output();
        Builtins.setOutput(out);
        try {
            while (true) {
                out.print(PROMPT);
                out.flush();

                String line = reader.readLine();
                if (line == null) {
                    return;
                }

                Parser parser = new Parser(new Lexer(line));
                Program program = parser.parseProgram();

                if (!parser.errors().isEmpty()) {
                    printParserErrors(out, parser.errors());
                    continue;
                }

                MonkeyObject evaluated;
                try {
                    Program withoutMacroDefinitions = MacroExpansion.defineMacro(program, macroEnv);
                    Program expanded = MacroExpansion.expandMacros(withoutMacroDefinitions, macroEnv);
                    evaluated = Evaluator.eval(expanded, env);
                } catch (RuntimeException e) {
                    // a macro that cannot be expanded, or a Java-level failure such as division by zero;
                    // keep the REPL alive instead of exiting
                    out.println("evaluation failed: " + e.getMessage());
                    continue;
                }

                if (evaluated != null) {
                    out.println(evaluated.inspect());
                }
            }
        } finally {
            Builtins.setOutput(previousOutput);
        }
    }

    private static void printParserErrors(PrintStream out, List<String> errors) {
        out.print(MONKEY_FACE);
        out.println("Woops! We ran into some monkey business here!");
        out.println(" parser errors:");
        for (String message : errors) {
            out.println("\t" + message);
        }
    }
}
