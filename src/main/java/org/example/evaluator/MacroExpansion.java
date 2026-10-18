package org.example.evaluator;

import org.example.ast.*;
import org.example.object.Environment;
import org.example.object.ErrorObject;
import org.example.object.MacroObject;
import org.example.object.MonkeyObject;
import org.example.object.QuoteObject;
import org.example.token.Token;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

public final class MacroExpansion {

    private MacroExpansion() {
    }

    public static Program defineMacro(Program program, Environment env) {
        List<Statement> remaining = new ArrayList<>();

        for (Statement statement : program.statements()) {
            if (statement instanceof LetStatement(Token _, Identifier name, Expression value) && value instanceof MacroLiteral macroLiteral) {
                addMacro(name, macroLiteral, env);
            } else {
                remaining.add(statement);
            }
        }

        return new Program(remaining);
    }

    private static void addMacro(Identifier name, MacroLiteral macroLiteral, Environment env) {
        MacroObject macroObject = new MacroObject(macroLiteral.parameters(), macroLiteral.body(), env);
        env.set(name.value(), macroObject);
    }

    // Replaces every call to a macro defined in env with the code that macro returns.
    public static Program expandMacros(Program program, Environment env) {
        UnaryOperator<Expression> func = node -> {
            if (!(node instanceof CallExpression(Token _, Expression function, List<Expression> arguments))) {
                return node;
            }

            if (!(function instanceof Identifier(Token _, String value) && env.get(value).orElse(null) instanceof MacroObject(List<Identifier> macroParams, BlockStatement macroBody, Environment macroEnv))) {
                return node;
            }

            List<QuoteObject> args = arguments.stream().map(QuoteObject::new).toList();

            if (args.size() != macroParams.size()) {
                throw expansionFailure(value, "wrong number of arguments. got=%d, want=%d"
                        .formatted(args.size(), macroParams.size()));
            }
            Environment extendedMacroEnv = new Environment(macroEnv);
            for (int i = 0; i < macroParams.size(); i++) {
                extendedMacroEnv.set(macroParams.get(i).value(), args.get(i));
            }

            MonkeyObject evaluated = Evaluator.eval(macroBody, extendedMacroEnv);

            return switch (evaluated) {
                case QuoteObject(Expression quoteNode) -> quoteNode;
                case ErrorObject error -> throw expansionFailure(value, error.message());
                case null -> throw expansionFailure(value, "macro must return quoted code, got nothing");
                default -> throw expansionFailure(value, "macro must return quoted code, got " + evaluated.type());
            };
        };

        return Modifier.modify(program, func);
    }

    private static RuntimeException expansionFailure(String macroName, String reason) {
        return new RuntimeException("cannot expand macro " + macroName + ": " + reason);
    }

}
