package org.example.ast;


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public final class Modifier {

    private Modifier() {
    }

    // Walks the whole tree and returns a new tree in which every node has been passed through modifier.
    // Records can't be changed in place, so changed nodes are rebuilt and the original tree is left as it was.
    public static Node modify(Node node, UnaryOperator<Expression> modifier) {
        return switch (node) {
            case Program program -> modifyProgram(program, modifier);
            case Statement statement -> modifyStatement(statement, modifier);
            case Expression expression -> modifyExpression(expression, modifier);
        };
    }

    public static Expression modify(Expression expression, UnaryOperator<Expression> modifier) {
        return modifyExpression(expression, modifier);
    }

    public static Program modify(Program program, UnaryOperator<Expression> modifier) {
        return modifyProgram(program, modifier);
    }

    private static Expression modifyExpression(Expression expression, UnaryOperator<Expression> modifier) {
        Expression rebuilt = switch (expression) {
            case Identifier _, NumberLiteral _, BooleanLiteral _, StringLiteral _ -> expression;
            case InfixExpression infixExpression -> new InfixExpression(infixExpression.token(), modifyExpression(infixExpression.left(), modifier), infixExpression.operator(), modifyExpression(infixExpression.right(), modifier));
            case PrefixExpression prefixExpression -> new PrefixExpression(prefixExpression.token(), prefixExpression.operator(), modifyExpression(prefixExpression.right(), modifier));
            case IfExpression ifExpression -> new IfExpression(ifExpression.token(),
                    modifyExpression(ifExpression.condition(), modifier),
                    modifyBlockStatement(ifExpression.consequence(), modifier),
                    ifExpression.alternative() != null ? modifyBlockStatement(ifExpression.alternative(), modifier) : null);
            case FunctionLiteral functionLiteral -> new FunctionLiteral(functionLiteral.token(),
                    functionLiteral.parameters(),
                    modifyBlockStatement(functionLiteral.body(), modifier));
            case ArrayLiteral arrayLiteral-> new ArrayLiteral(arrayLiteral.token(), arrayLiteral.elements().stream()
                    .map(e -> modifyExpression(e, modifier))
                    .collect(Collectors.toList()));
            case HashLiteral hashLiteral -> new HashLiteral(hashLiteral.token(), hashLiteral.pairs().entrySet().stream()
                    .collect(Collectors.toMap(
                            e -> modifyExpression(e.getKey(), modifier),
                            e -> modifyExpression(e.getValue(), modifier),
                            (a, b) -> b,
                            LinkedHashMap::new)));
            case IndexExpression indexExpression -> new IndexExpression(indexExpression.token(), modifyExpression(indexExpression.left(), modifier), modifyExpression(indexExpression.index(), modifier));
            case CallExpression callExpression -> new CallExpression(callExpression.token(), modifyExpression(callExpression.function(), modifier), callExpression.arguments().stream().map(a -> modifyExpression(a, modifier)).collect(Collectors.toList()));
            case MacroLiteral macroLiteral -> new MacroLiteral(macroLiteral.token(), macroLiteral.parameters(), modifyBlockStatement(macroLiteral.body(), modifier));
        };
        return modifier.apply(rebuilt);
    }

    private static BlockStatement modifyBlockStatement(BlockStatement s, UnaryOperator<Expression> modifier) {
        return new BlockStatement(s.token(), s.statements().stream().map(s1 -> modifyStatement(s1, modifier)).collect(Collectors.toList()));
    }

    private static Statement modifyStatement(Statement statement, UnaryOperator<Expression> modifier) {
        return switch (statement) {
            case ExpressionStatement s -> new ExpressionStatement(s.token(), modifyExpression(s.expression(), modifier));
            case BlockStatement s -> modifyBlockStatement(s, modifier);
            case ReturnStatement s -> new ReturnStatement(s.token(), modifyExpression(s.value(), modifier));
            case LetStatement s -> new LetStatement(s.token(), s.name(), modifyExpression(s.value(), modifier));
        };
    }

    private static Program modifyProgram(Program program, UnaryOperator<Expression> modifier) {
        List<Statement> statements = new ArrayList<>();

        for (Statement statement : program.statements()) {
            statements.add(modifyStatement(statement, modifier));
        }

        return new Program(statements);
    }
}
