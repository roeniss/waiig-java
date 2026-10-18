package org.example.ast;

import org.example.token.Token;
import org.example.token.TokenType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class ModifierTest {

    private static final Token PLUS = new Token(TokenType.PLUS, "+");
    private static final Token MINUS = new Token(TokenType.MINUS, "-");
    private static final Token LBRACKET = new Token(TokenType.LBRACKET, "[");
    private static final Token LBRACE = new Token(TokenType.LBRACE, "{");
    private static final Token LPAREN = new Token(TokenType.LPAREN, "(");
    private static final Token IF = new Token(TokenType.IF, "if");
    private static final Token RETURN = new Token(TokenType.RETURN, "return");
    private static final Token LET = new Token(TokenType.LET, "let");
    private static final Token FUNCTION = new Token(TokenType.FUNCTION, "fn");
    private static final Token STATEMENT = new Token(TokenType.IDENTIFIER, "x");

    // turns every number literal 1 into 2 and leaves every other node alone
    private static final UnaryOperator<Expression> TURN_ONE_INTO_TWO =
            expression -> expression instanceof NumberLiteral literal && literal.value() == 1 ? two() : expression;

    static Stream<Arguments> nodes() {
        return Stream.of(
                arguments("number literal",
                        one(),
                        two()),
                arguments("program",
                        new Program(List.of(statement(one()))),
                        new Program(List.of(statement(two())))),
                arguments("infix expression, left",
                        infix(one(), two()),
                        infix(two(), two())),
                arguments("infix expression, right",
                        infix(two(), one()),
                        infix(two(), two())),
                arguments("prefix expression",
                        new PrefixExpression(MINUS, "-", one()),
                        new PrefixExpression(MINUS, "-", two())),
                arguments("index expression",
                        new IndexExpression(LBRACKET, one(), one()),
                        new IndexExpression(LBRACKET, two(), two())),
                arguments("if expression",
                        new IfExpression(IF, one(), block(statement(one())), block(statement(one()))),
                        new IfExpression(IF, two(), block(statement(two())), block(statement(two())))),
                arguments("if expression without else",
                        new IfExpression(IF, one(), block(statement(one())), null),
                        new IfExpression(IF, two(), block(statement(two())), null)),
                arguments("return statement",
                        new ReturnStatement(RETURN, one()),
                        new ReturnStatement(RETURN, two())),
                arguments("let statement",
                        new LetStatement(LET, identifier("x"), one()),
                        new LetStatement(LET, identifier("x"), two())),
                arguments("function literal",
                        new FunctionLiteral(FUNCTION, List.of(), block(statement(one()))),
                        new FunctionLiteral(FUNCTION, List.of(), block(statement(two())))),
                arguments("array literal",
                        new ArrayLiteral(LBRACKET, List.of(one(), one())),
                        new ArrayLiteral(LBRACKET, List.of(two(), two()))),
                arguments("hash literal",
                        new HashLiteral(LBRACE, pairs(one(), one(), string("a"), one())),
                        new HashLiteral(LBRACE, pairs(two(), two(), string("a"), two()))),
                // not in WAIIG: its Modify skips call expressions, so unquote inside call arguments is never expanded
                arguments("call expression",
                        new CallExpression(LPAREN, identifier("add"), List.of(one(), one())),
                        new CallExpression(LPAREN, identifier("add"), List.of(two(), two())))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("nodes")
    void testModify(String description, Node input, Node expected) {
        Node modified = Modifier.modify(input, TURN_ONE_INTO_TWO);

        assertEquals(expected, modified);
    }

    @Test
    void testModifyLeavesOriginalTreeUnchanged() {
        // mutable lists like the ones the parser builds, so changing them in place would show up here
        Program original = new Program(new ArrayList<>(List.of(
                statement(new ArrayLiteral(LBRACKET, new ArrayList<>(List.of(one())))))));
        Program copy = new Program(List.of(
                statement(new ArrayLiteral(LBRACKET, List.of(one())))));

        Modifier.modify(original, TURN_ONE_INTO_TWO);

        assertEquals(copy, original);
    }

    private static NumberLiteral one() {
        return new NumberLiteral(new Token(TokenType.NUMBER, "1"), 1);
    }

    private static NumberLiteral two() {
        return new NumberLiteral(new Token(TokenType.NUMBER, "2"), 2);
    }

    private static InfixExpression infix(Expression left, Expression right) {
        return new InfixExpression(PLUS, left, "+", right);
    }

    private static Identifier identifier(String name) {
        return new Identifier(new Token(TokenType.IDENTIFIER, name), name);
    }

    private static StringLiteral string(String value) {
        return new StringLiteral(new Token(TokenType.STRING, value), value);
    }

    // a fixed token: modify keeps a statement's own token, so it must not depend on the expression being replaced
    private static ExpressionStatement statement(Expression expression) {
        return new ExpressionStatement(STATEMENT, expression);
    }

    private static BlockStatement block(Statement... statements) {
        return new BlockStatement(LBRACE, List.of(statements));
    }

    private static Map<Expression, Expression> pairs(Expression... keysAndValues) {
        Map<Expression, Expression> pairs = new LinkedHashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            pairs.put(keysAndValues[i], keysAndValues[i + 1]);
        }
        return pairs;
    }
}
