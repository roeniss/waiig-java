package org.example.object;

import org.example.ast.ExpressionStatement;
import org.example.ast.FunctionLiteral;
import org.example.ast.Program;
import org.example.lexer.Lexer;
import org.example.parser.Parser;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class FunctionObjectTest {

    static Stream<Arguments> functions() {
        return Stream.of(
                arguments("fn(x) { x + 2; }", "fn(x) {\n(x + 2)\n}"),
                arguments("fn(x, y) { x + y; }", "fn(x, y) {\n(x + y)\n}"),
                arguments("fn() { 1 }", "fn() {\n1\n}"),
                arguments("fn() {}", "fn() {\n\n}")
        );
    }

    @ParameterizedTest
    @MethodSource("functions")
    void testInspect(String source, String expected) {
        FunctionLiteral literal = parseFunctionLiteral(source);
        FunctionObject function = new FunctionObject(literal.parameters(), literal.body(), new Environment());

        assertEquals(expected, function.inspect());
    }

    private static FunctionLiteral parseFunctionLiteral(String source) {
        Parser parser = new Parser(new Lexer(source));
        Program program = parser.parseProgram();
        assertEquals(List.of(), parser.errors(), "parser errors");

        ExpressionStatement statement = (ExpressionStatement) program.statements().getFirst();
        return (FunctionLiteral) statement.expression();
    }
}
