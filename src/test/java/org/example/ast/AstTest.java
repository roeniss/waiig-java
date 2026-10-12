package org.example.ast;

import org.example.token.Token;
import org.example.token.TokenType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class AstTest {

    private static final Token LET = new Token(TokenType.LET, "let");
    private static final Token RETURN = new Token(TokenType.RETURN, "return");
    private static final Token PLUS = new Token(TokenType.PLUS, "+");
    private static final Token MINUS = new Token(TokenType.MINUS, "-");
    private static final Token BANG = new Token(TokenType.BANG, "!");
    private static final Token ASTERISK = new Token(TokenType.ASTERISK, "*");
    private static final Token EQ = new Token(TokenType.EQ, "==");
    private static final Token LT = new Token(TokenType.LT, "<");
    private static final Token IF = new Token(TokenType.IF, "if");
    private static final Token LBRACE = new Token(TokenType.LBRACE, "{");
    private static final Token FUNCTION = new Token(TokenType.FUNCTION, "fn");
    private static final Token LPAREN = new Token(TokenType.LPAREN, "(");

    static Stream<Arguments> nodes() {
        return Stream.of(
                arguments("identifier",
                        identifier("foobar"),
                        "foobar"),
                arguments("number literal",
                        new NumberLiteral(new Token(TokenType.NUMBER, "5"), 5),
                        "5"),
                arguments("boolean literal true",
                        booleanLiteral(true),
                        "true"),
                arguments("boolean literal false",
                        booleanLiteral(false),
                        "false"),
                arguments("prefix expression with boolean",
                        new PrefixExpression(BANG, "!", booleanLiteral(true)),
                        "(!true)"),
                arguments("infix expression with booleans",
                        new InfixExpression(EQ, booleanLiteral(true), "==", booleanLiteral(false)),
                        "(true == false)"),
                arguments("prefix expression",
                        new PrefixExpression(MINUS, "-", identifier("a")),
                        "(-a)"),
                arguments("infix expression",
                        new InfixExpression(PLUS, identifier("a"), "+", identifier("b")),
                        "(a + b)"),
                arguments("nested prefix expression",
                        new PrefixExpression(BANG, "!",
                                new PrefixExpression(MINUS, "-", identifier("a"))),
                        "(!(-a))"),
                arguments("nested infix expression",
                        new InfixExpression(ASTERISK,
                                new InfixExpression(PLUS, identifier("a"), "+", identifier("b")),
                                "*",
                                identifier("c")),
                        "((a + b) * c)"),
                arguments("prefix expression inside infix expression",
                        new InfixExpression(ASTERISK,
                                new PrefixExpression(MINUS, "-", identifier("a")),
                                "*",
                                identifier("b")),
                        "((-a) * b)"),
                arguments("block statement",
                        block(expressionStatement(identifier("x"))),
                        "x"),
                arguments("block statement with multiple statements",
                        block(expressionStatement(identifier("x")), expressionStatement(identifier("y"))),
                        "xy"),
                arguments("empty block statement",
                        block(),
                        ""),
                // the book's String() puts no space after "if" and none before "else"
                arguments("if expression",
                        new IfExpression(IF,
                                new InfixExpression(LT, identifier("x"), "<", identifier("y")),
                                block(expressionStatement(identifier("x"))),
                                null),
                        "if(x < y) x"),
                arguments("if-else expression",
                        new IfExpression(IF,
                                new InfixExpression(LT, identifier("x"), "<", identifier("y")),
                                block(expressionStatement(identifier("x"))),
                                block(expressionStatement(identifier("y")))),
                        "if(x < y) xelse y"),
                arguments("function literal",
                        new FunctionLiteral(FUNCTION,
                                List.of(identifier("x"), identifier("y")),
                                block(expressionStatement(
                                        new InfixExpression(PLUS, identifier("x"), "+", identifier("y"))))),
                        "fn(x, y) (x + y)"),
                arguments("function literal without parameters and body",
                        new FunctionLiteral(FUNCTION, List.of(), block()),
                        "fn() "),
                arguments("function literal with null parameters",
                        new FunctionLiteral(FUNCTION, null, block()),
                        "fn() "),
                arguments("let statement with function literal value",
                        new LetStatement(LET, identifier("add"),
                                new FunctionLiteral(FUNCTION,
                                        List.of(identifier("x"), identifier("y")),
                                        block(expressionStatement(
                                                new InfixExpression(PLUS, identifier("x"), "+", identifier("y")))))),
                        "let add = fn(x, y) (x + y);"),
                arguments("call expression",
                        new CallExpression(LPAREN, identifier("add"),
                                List.of(number(1), new InfixExpression(ASTERISK, number(2), "*", number(3)))),
                        "add(1, (2 * 3))"),
                arguments("call expression without arguments",
                        new CallExpression(LPAREN, identifier("add"), List.of()),
                        "add()"),
                arguments("call expression with null arguments",
                        new CallExpression(LPAREN, identifier("add"), null),
                        "add()"),
                arguments("call expression inside infix expression",
                        new InfixExpression(PLUS, identifier("a"), "+",
                                new CallExpression(LPAREN, identifier("add"), List.of(identifier("b")))),
                        "(a + add(b))"),
                arguments("call expression on function literal",
                        new CallExpression(LPAREN,
                                new FunctionLiteral(FUNCTION, List.of(identifier("x")),
                                        block(expressionStatement(identifier("x")))),
                                List.of(number(5))),
                        "fn(x) x(5)"),
                arguments("let statement with infix value",
                        new LetStatement(LET, identifier("x"),
                                new InfixExpression(PLUS, identifier("a"), "+", identifier("b"))),
                        "let x = (a + b);"),
                arguments("let statement",
                        new LetStatement(LET, identifier("myVar"), identifier("anotherVar")),
                        "let myVar = anotherVar;"),
                arguments("let statement without value",
                        new LetStatement(LET, identifier("x"), null),
                        "let x = ;"),
                arguments("return statement",
                        new ReturnStatement(RETURN, identifier("x")),
                        "return x;"),
                arguments("return statement without value",
                        new ReturnStatement(RETURN, null),
                        "return ;"),
                arguments("expression statement",
                        new ExpressionStatement(new Token(TokenType.IDENTIFIER, "x"), identifier("x")),
                        "x"),
                arguments("expression statement without expression",
                        new ExpressionStatement(new Token(TokenType.IDENTIFIER, "x"), null),
                        ""),
                arguments("program (TestString in the book)",
                        new Program(List.of(
                                new LetStatement(LET, identifier("myVar"), identifier("anotherVar")))),
                        "let myVar = anotherVar;"),
                // statements are concatenated without a separator, as in the book
                arguments("program with multiple statements",
                        new Program(List.of(
                                new LetStatement(LET, identifier("x"), identifier("y")),
                                new ReturnStatement(RETURN, identifier("x")))),
                        "let x = y;return x;"),
                arguments("empty program",
                        new Program(List.of()),
                        "")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("nodes")
    void testToString(String description, Node node, String expected) {
        assertEquals(expected, node.toString());
    }

    private static Identifier identifier(String name) {
        return new Identifier(new Token(TokenType.IDENTIFIER, name), name);
    }

    private static NumberLiteral number(long value) {
        return new NumberLiteral(new Token(TokenType.NUMBER, String.valueOf(value)), value);
    }

    private static ExpressionStatement expressionStatement(Expression expression) {
        return new ExpressionStatement(expression.token(), expression);
    }

    private static BlockStatement block(Statement... statements) {
        return new BlockStatement(LBRACE, List.of(statements));
    }

    private static BooleanLiteral booleanLiteral(boolean value) {
        TokenType type = value ? TokenType.TRUE : TokenType.FALSE;
        return new BooleanLiteral(new Token(type, String.valueOf(value)), value);
    }
}
