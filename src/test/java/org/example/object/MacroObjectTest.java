package org.example.object;

import org.example.ast.ExpressionStatement;
import org.example.ast.MacroLiteral;
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

class MacroObjectTest {

    static Stream<Arguments> macros() {
        return Stream.of(
                arguments("macro(x, y) { x + y; }", "macro(x, y) {\n(x + y)\n}"),
                arguments("macro(x) { quote(unquote(x)) }", "macro(x) {\nquote(unquote(x))\n}"),
                arguments("macro() {}", "macro() {\n\n}")
        );
    }

    @ParameterizedTest
    @MethodSource("macros")
    void testInspect(String source, String expected) {
        MacroLiteral literal = parseMacroLiteral(source);
        MacroObject macro = new MacroObject(literal.parameters(), literal.body(), new Environment());

        assertEquals(expected, macro.inspect());
    }

    private static MacroLiteral parseMacroLiteral(String source) {
        Parser parser = new Parser(new Lexer(source));
        Program program = parser.parseProgram();
        assertEquals(List.of(), parser.errors(), "parser errors");

        ExpressionStatement statement = (ExpressionStatement) program.statements().getFirst();
        return (MacroLiteral) statement.expression();
    }
}
