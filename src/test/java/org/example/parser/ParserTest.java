package org.example.parser;

import org.example.ast.ArrayLiteral;
import org.example.ast.BlockStatement;
import org.example.ast.BooleanLiteral;
import org.example.ast.CallExpression;
import org.example.ast.Expression;
import org.example.ast.ExpressionStatement;
import org.example.ast.FunctionLiteral;
import org.example.ast.HashLiteral;
import org.example.ast.Identifier;
import org.example.ast.IfExpression;
import org.example.ast.InfixExpression;
import org.example.ast.IndexExpression;
import org.example.ast.NumberLiteral;
import org.example.ast.LetStatement;
import org.example.ast.PrefixExpression;
import org.example.ast.Program;
import org.example.ast.ReturnStatement;
import org.example.ast.Statement;
import org.example.ast.StringLiteral;
import org.example.lexer.Lexer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class ParserTest {

    @Test
    void testLetStatements() {
        String input = """
                let x = 5;
                let y = 10;
                let foobar = 838383;
                """;

        Parser parser = new Parser(new Lexer(input));
        Program program = parser.parseProgram();
        assertNoParserErrors(parser);

        assertNotNull(program, "parseProgram() returned null");
        assertEquals(3, program.statements().size(), "program.statements() size");

        List<String> expectedNames = List.of("x", "y", "foobar");

        List<Executable> assertions = new ArrayList<>();
        for (int i = 0; i < expectedNames.size(); i++) {
            Statement statement = program.statements().get(i);
            String name = expectedNames.get(i);
            String message = "statements[" + i + "]";
            assertions.add(() -> assertLetStatement(statement, name, message));
        }
        assertAll(assertions);
    }

    @Test
    void testLetStatementErrors() {
        String input = """
                let x 5;
                let = 10;
                let 838383;
                """;

        Parser parser = new Parser(new Lexer(input));
        parser.parseProgram();

        assertEquals(List.of(
                "expected next token to be ASSIGN, got NUMBER instead",
                "expected next token to be IDENTIFIER, got ASSIGN instead",
                // the leftover "= 10;" is parsed as an expression statement starting with "="
                "no prefix parse function for ASSIGN found",
                "expected next token to be IDENTIFIER, got NUMBER instead"
        ), parser.errors());
    }

    @Test
    void testReturnStatements() {
        String input = """
                return 5;
                return 10;
                return 993322;
                """;

        Parser parser = new Parser(new Lexer(input));
        Program program = parser.parseProgram();
        assertNoParserErrors(parser);

        assertEquals(3, program.statements().size(), "program.statements() size");

        List<Executable> assertions = new ArrayList<>();
        for (int i = 0; i < program.statements().size(); i++) {
            Statement statement = program.statements().get(i);
            String message = "statements[" + i + "]";
            assertions.add(() -> assertReturnStatement(statement, message));
        }
        assertAll(assertions);
    }

    static Stream<Arguments> letStatementValues() {
        return Stream.of(
                arguments("let x = 5;", "x", 5),
                arguments("let y = true;", "y", true),
                arguments("let foobar = y;", "foobar", "y")
        );
    }

    @ParameterizedTest
    @MethodSource("letStatementValues")
    void testLetStatementValues(String input, String name, Object value) {
        Statement statement = parseSingleStatement(input);

        assertLetStatement(statement, name, input);
        assertLiteralExpression(((LetStatement) statement).value(), value);
    }

    static Stream<Arguments> returnStatementValues() {
        return Stream.of(
                arguments("return 5;", 5),
                arguments("return true;", true),
                arguments("return foobar;", "foobar")
        );
    }

    @ParameterizedTest
    @MethodSource("returnStatementValues")
    void testReturnStatementValues(String input, Object value) {
        Statement statement = parseSingleStatement(input);

        assertReturnStatement(statement, input);
        assertLiteralExpression(((ReturnStatement) statement).value(), value);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            let x = 1 + 2 * 3;             | let x = (1 + (2 * 3));
            let add = fn(x, y) { x + y; }; | let add = fn(x, y) (x + y);
            return add(1, 2 * 3);          | return add(1, (2 * 3));
            return if (x < y) { x } else { y }; | return if(x < y) xelse y;
            """)
    void testStatementsWithComplexValues(String input, String expected) {
        assertEquals(expected, parse(input).toString());
    }

    @Test
    void testStatementsWithoutSemicolon() {
        // the old "skip until ;" TODO loop never ends without a semicolon, so fail after 1s instead of hanging
        Program program = assertTimeoutPreemptively(Duration.ofSeconds(1), () -> parse("let x = 5\nreturn x"));

        assertEquals("let x = 5;return x;", program.toString());
    }

    @Test
    void testIdentifierExpression() {
        Expression expression = parseSingleExpression("foobar;");

        assertIdentifier(expression, "foobar");
    }

    @Test
    void testNumberLiteralExpression() {
        Expression expression = parseSingleExpression("5;");

        assertNumberLiteral(expression, 5);
    }

    @Test
    void testStringLiteralExpression() {
        Expression expression = parseSingleExpression("\"hello world\";");

        StringLiteral literal = assertInstanceOf(StringLiteral.class, expression);
        assertEquals("hello world", literal.value());
        assertEquals("hello world", literal.toString());
    }

    @Test
    void testNumberLiteralMaxValue() {
        Expression expression = parseSingleExpression("9223372036854775807;");

        assertNumberLiteral(expression, Long.MAX_VALUE);
    }

    @Test
    void testNumberLiteralOverflowError() {
        Parser parser = new Parser(new Lexer("9223372036854775808;"));
        parser.parseProgram();

        assertEquals(List.of("Invalid number literal: 9223372036854775808"), parser.errors());
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            true;  | true
            false; | false
            """)
    void testBooleanExpression(String input, boolean expected) {
        Expression expression = parseSingleExpression(input);

        assertBooleanLiteral(expression, expected);
    }

    static Stream<Arguments> prefixExpressions() {
        return Stream.of(
                arguments("!5;", "!", 5),
                arguments("-15;", "-", 15),
                arguments("!true;", "!", true),
                arguments("!false;", "!", false)
        );
    }

    @ParameterizedTest
    @MethodSource("prefixExpressions")
    void testPrefixExpressions(String input, String operator, Object value) {
        Expression expression = parseSingleExpression(input);

        PrefixExpression prefix = assertInstanceOf(PrefixExpression.class, expression);
        assertEquals(operator, prefix.operator());
        assertLiteralExpression(prefix.right(), value);
    }

    static Stream<Arguments> infixExpressions() {
        return Stream.of(
                arguments("5 + 5;", 5, "+", 5),
                arguments("5 - 5;", 5, "-", 5),
                arguments("5 * 5;", 5, "*", 5),
                arguments("5 / 5;", 5, "/", 5),
                arguments("5 > 5;", 5, ">", 5),
                arguments("5 < 5;", 5, "<", 5),
                arguments("5 == 5;", 5, "==", 5),
                arguments("5 != 5;", 5, "!=", 5),
                arguments("alice * bob;", "alice", "*", "bob"),
                arguments("true == true", true, "==", true),
                arguments("true != false", true, "!=", false),
                arguments("false == false", false, "==", false)
        );
    }

    @ParameterizedTest
    @MethodSource("infixExpressions")
    void testInfixExpressions(String input, Object left, String operator, Object right) {
        Expression expression = parseSingleExpression(input);

        assertInfixExpression(expression, left, operator, right);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            -a * b                     | ((-a) * b)
            !-a                        | (!(-a))
            a + b + c                  | ((a + b) + c)
            a + b - c                  | ((a + b) - c)
            a * b * c                  | ((a * b) * c)
            a * b / c                  | ((a * b) / c)
            a + b / c                  | (a + (b / c))
            a + b * c + d / e - f      | (((a + (b * c)) + (d / e)) - f)
            3 + 4; -5 * 5              | (3 + 4)((-5) * 5)
            5 > 4 == 3 < 4             | ((5 > 4) == (3 < 4))
            5 < 4 != 3 > 4             | ((5 < 4) != (3 > 4))
            3 + 4 * 5 == 3 * 1 + 4 * 5 | ((3 + (4 * 5)) == ((3 * 1) + (4 * 5)))
            true                       | true
            false                      | false
            3 > 5 == false             | ((3 > 5) == false)
            3 < 5 == true              | ((3 < 5) == true)
            1 + (2 + 3) + 4            | ((1 + (2 + 3)) + 4)
            (5 + 5) * 2                | ((5 + 5) * 2)
            2 / (5 + 5)                | (2 / (5 + 5))
            -(5 + 5)                   | (-(5 + 5))
            !(true == true)            | (!(true == true))
            a + add(b * c) + d         | ((a + add((b * c))) + d)
            add(a, b, 1, 2 * 3, 4 + 5, add(6, 7 * 8)) | add(a, b, 1, (2 * 3), (4 + 5), add(6, (7 * 8)))
            add(a + b + c * d / f + g) | add((((a + b) + ((c * d) / f)) + g))
            add(1)(2)                  | add(1)(2)
            """)
    void testOperatorPrecedenceParsing(String input, String expected) {
        assertEquals(expected, parse(input).toString());
    }

    @Test
    void testGroupedExpressionMissingRightParen() {
        Parser parser = new Parser(new Lexer("(1 + 2"));
        parser.parseProgram();

        assertEquals(List.of("expected next token to be RPAREN, got EOF instead"), parser.errors());
    }

    @Test
    void testIfExpression() {
        Expression expression = parseSingleExpression("if (x < y) { x }");

        IfExpression ifExpression = assertInstanceOf(IfExpression.class, expression);
        assertInfixExpression(ifExpression.condition(), "x", "<", "y");
        assertSingleIdentifierBlock(ifExpression.consequence(), "x");
        assertNull(ifExpression.alternative(), "alternative");
    }

    @Test
    void testIfElseExpression() {
        Expression expression = parseSingleExpression("if (x < y) { x } else { y }");

        IfExpression ifExpression = assertInstanceOf(IfExpression.class, expression);
        assertInfixExpression(ifExpression.condition(), "x", "<", "y");
        assertSingleIdentifierBlock(ifExpression.consequence(), "x");
        assertSingleIdentifierBlock(ifExpression.alternative(), "y");
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            if (x) {}         | 0
            if (x) { x }      | 1
            if (x) { x; y }   | 2
            if (x) { x; y; }  | 2
            """)
    void testBlockStatementSize(String input, int expectedSize) {
        Expression expression = parseSingleExpression(input);

        IfExpression ifExpression = assertInstanceOf(IfExpression.class, expression);
        assertEquals(expectedSize, ifExpression.consequence().statements().size(), "consequence.statements() size");
    }

    @Test
    void testIfExpressionMissingLeftParen() {
        Parser parser = new Parser(new Lexer("if x { x }"));
        parser.parseProgram();

        // the rest of the input produces more errors after this one; only the first is the real cause
        assertEquals("expected next token to be LPAREN, got IDENTIFIER instead", parser.errors().getFirst());
    }

    @Test
    void testFunctionLiteralParsing() {
        Expression expression = parseSingleExpression("fn(x, y) { x + y; }");

        FunctionLiteral function = assertInstanceOf(FunctionLiteral.class, expression);
        assertEquals(2, function.parameters().size(), "parameters size");
        assertLiteralExpression(function.parameters().get(0), "x");
        assertLiteralExpression(function.parameters().get(1), "y");

        assertEquals(1, function.body().statements().size(), "body.statements() size");
        ExpressionStatement bodyStatement = assertInstanceOf(ExpressionStatement.class, function.body().statements().getFirst());
        assertInfixExpression(bodyStatement.expression(), "x", "+", "y");
    }

    static Stream<Arguments> functionParameters() {
        return Stream.of(
                arguments("fn() {};", List.of()),
                arguments("fn(x) {};", List.of("x")),
                arguments("fn(x, y, z) {};", List.of("x", "y", "z"))
        );
    }

    @ParameterizedTest
    @MethodSource("functionParameters")
    void testFunctionParameterParsing(String input, List<String> expectedParameters) {
        Expression expression = parseSingleExpression(input);

        FunctionLiteral function = assertInstanceOf(FunctionLiteral.class, expression);
        assertEquals(expectedParameters.size(), function.parameters().size(), "parameters size");
        for (int i = 0; i < expectedParameters.size(); i++) {
            assertLiteralExpression(function.parameters().get(i), expectedParameters.get(i));
        }
    }

    @Test
    void testFunctionLiteralMissingRightParen() {
        Parser parser = new Parser(new Lexer("fn(x, y { x }"));
        parser.parseProgram();

        assertEquals("expected next token to be RPAREN, got LBRACE instead", parser.errors().getFirst());
    }

    @Test
    void testEmptyHashLiteralParsing() {
        HashLiteral hash = assertInstanceOf(HashLiteral.class, parseSingleExpression("{}"));

        assertEquals(Map.of(), hash.pairs());
    }

    @Test
    void testHashLiteralStringKeys() {
        HashLiteral hash = assertInstanceOf(
                HashLiteral.class,
                parseSingleExpression("{\"one\": 1, \"two\": 2, \"three\": 3}")
        );
        Map<String, Long> expected = Map.of("one", 1L, "two", 2L, "three", 3L);

        assertEquals(expected.size(), hash.pairs().size(), "pairs size");
        hash.pairs().forEach((keyExpression, valueExpression) -> {
            StringLiteral key = assertInstanceOf(StringLiteral.class, keyExpression);
            assertNumberLiteral(valueExpression, expected.get(key.value()));
        });
    }

    @Test
    void testHashLiteralExpressionValues() {
        HashLiteral hash = assertInstanceOf(
                HashLiteral.class,
                parseSingleExpression("{\"one\": 0 + 1, \"two\": 10 - 8, \"three\": 15 / 5}")
        );
        Map<String, String> expected = Map.of(
                "one", "(0 + 1)",
                "two", "(10 - 8)",
                "three", "(15 / 5)"
        );

        assertEquals(expected.size(), hash.pairs().size(), "pairs size");
        hash.pairs().forEach((keyExpression, valueExpression) -> {
            StringLiteral key = assertInstanceOf(StringLiteral.class, keyExpression);
            assertEquals(expected.get(key.value()), valueExpression.toString());
        });
    }

    @Test
    void testHashLiteralKeepsSourceOrder() {
        Expression expression = parseSingleExpression("{\"one\": 1, \"two\": 2, \"three\": 3, \"four\": 4}");

        assertEquals("{one: 1, two: 2, three: 3, four: 4}", expression.toString());
    }

    @Test
    void testArrayLiteralParsing() {
        ArrayLiteral array = assertInstanceOf(
                ArrayLiteral.class,
                parseSingleExpression("[1, 2 * 2, 3 + 3]")
        );

        assertEquals(3, array.elements().size(), "elements size");
        assertLiteralExpression(array.elements().get(0), 1);
        assertInfixExpression(array.elements().get(1), 2, "*", 2);
        assertInfixExpression(array.elements().get(2), 3, "+", 3);
    }

    @Test
    void testEmptyArrayLiteralParsing() {
        ArrayLiteral array = assertInstanceOf(ArrayLiteral.class, parseSingleExpression("[]"));

        assertEquals(List.of(), array.elements());
    }

    @Test
    void testIndexExpressionParsing() {
        IndexExpression index = assertInstanceOf(
                IndexExpression.class,
                parseSingleExpression("myArray[1 + 1]")
        );

        assertIdentifier(index.left(), "myArray");
        assertInfixExpression(index.index(), 1, "+", 1);
    }

    @Test
    void testCallExpressionParsing() {
        Expression expression = parseSingleExpression("add(1, 2 * 3, 4 + 5);");

        CallExpression call = assertInstanceOf(CallExpression.class, expression);
        assertIdentifier(call.function(), "add");
        assertEquals(3, call.arguments().size(), "arguments size");
        assertLiteralExpression(call.arguments().get(0), 1);
        assertInfixExpression(call.arguments().get(1), 2, "*", 3);
        assertInfixExpression(call.arguments().get(2), 4, "+", 5);
    }

    static Stream<Arguments> callArguments() {
        return Stream.of(
                arguments("add();", List.of()),
                arguments("add(1);", List.of("1")),
                arguments("add(1, 2 * 3, 4 + 5);", List.of("1", "(2 * 3)", "(4 + 5)"))
        );
    }

    @ParameterizedTest
    @MethodSource("callArguments")
    void testCallExpressionParameterParsing(String input, List<String> expectedArguments) {
        Expression expression = parseSingleExpression(input);

        CallExpression call = assertInstanceOf(CallExpression.class, expression);
        assertIdentifier(call.function(), "add");
        assertEquals(expectedArguments, call.arguments().stream().map(Expression::toString).toList());
    }

    @Test
    void testCallExpressionOnFunctionLiteral() {
        Expression expression = parseSingleExpression("fn(x) { x }(5)");

        CallExpression call = assertInstanceOf(CallExpression.class, expression);
        assertInstanceOf(FunctionLiteral.class, call.function());
        assertEquals(1, call.arguments().size(), "arguments size");
        assertLiteralExpression(call.arguments().getFirst(), 5);
    }

    @Test
    void testCallExpressionMissingRightParen() {
        Parser parser = new Parser(new Lexer("add(1, 2"));
        parser.parseProgram();

        assertEquals("expected next token to be RPAREN, got EOF instead", parser.errors().getFirst());
    }

    @Test
    void testNoPrefixParseFunctionError() {
        Parser parser = new Parser(new Lexer("+5;"));
        parser.parseProgram();

        assertEquals(List.of("no prefix parse function for PLUS found"), parser.errors());
    }

    private static Program parse(String input) {
        Parser parser = new Parser(new Lexer(input));
        Program program = parser.parseProgram();
        assertNoParserErrors(parser);
        return program;
    }

    private static Statement parseSingleStatement(String input) {
        Program program = parse(input);
        assertEquals(1, program.statements().size(), "program.statements() size");
        return program.statements().getFirst();
    }

    private static Expression parseSingleExpression(String input) {
        ExpressionStatement statement = assertInstanceOf(ExpressionStatement.class, parseSingleStatement(input));
        return statement.expression();
    }

    private static void assertNoParserErrors(Parser parser) {
        assertEquals(List.of(), parser.errors(), "parser errors");
    }

    private static void assertIdentifier(Expression expression, String name) {
        Identifier identifier = assertInstanceOf(Identifier.class, expression);
        assertEquals(name, identifier.value());
        assertEquals(name, identifier.tokenLiteral());
    }

    private static void assertNumberLiteral(Expression expression, long value) {
        NumberLiteral literal = assertInstanceOf(NumberLiteral.class, expression);
        assertEquals(value, literal.value());
        assertEquals(String.valueOf(value), literal.tokenLiteral());
    }

    private static void assertBooleanLiteral(Expression expression, boolean value) {
        BooleanLiteral literal = assertInstanceOf(BooleanLiteral.class, expression);
        assertEquals(value, literal.value());
        assertEquals(String.valueOf(value), literal.tokenLiteral());
    }

    // testLiteralExpression in the book: picks the right check from the type of `expected`
    private static void assertLiteralExpression(Expression expression, Object expected) {
        switch (expected) {
            case Integer value -> assertNumberLiteral(expression, value);
            case Long value -> assertNumberLiteral(expression, value);
            case String value -> assertIdentifier(expression, value);
            case Boolean value -> assertBooleanLiteral(expression, value);
            default -> fail("type of expected not handled: " + expected.getClass());
        }
    }

    private static void assertSingleIdentifierBlock(BlockStatement block, String name) {
        assertNotNull(block, "block");
        assertEquals(1, block.statements().size(), "block.statements() size");

        ExpressionStatement statement = assertInstanceOf(ExpressionStatement.class, block.statements().getFirst());
        assertIdentifier(statement.expression(), name);
    }

    private static void assertInfixExpression(Expression expression, Object left, String operator, Object right) {
        InfixExpression infix = assertInstanceOf(InfixExpression.class, expression);
        assertLiteralExpression(infix.left(), left);
        assertEquals(operator, infix.operator());
        assertLiteralExpression(infix.right(), right);
    }

    private static void assertLetStatement(Statement statement, String name, String message) {
        assertEquals("let", statement.tokenLiteral(), message);

        LetStatement letStatement = assertInstanceOf(LetStatement.class, statement, message);
        assertEquals(name, letStatement.name().value(), message);
        assertEquals(name, letStatement.name().tokenLiteral(), message);
    }

    private static void assertReturnStatement(Statement statement, String message) {
        ReturnStatement returnStatement = assertInstanceOf(ReturnStatement.class, statement, message);
        assertEquals("return", returnStatement.tokenLiteral(), message);
    }
}
