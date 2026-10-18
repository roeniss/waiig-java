package org.example.evaluator;

import org.example.ast.Program;
import org.example.lexer.Lexer;
import org.example.object.Environment;
import org.example.object.MacroObject;
import org.example.object.MonkeyObject;
import org.example.parser.Parser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class MacroExpansionTest {

    @Test
    void testDefineMacros() {
        Program program = parse("""
                let number = 1;
                let function = fn(x, y) { x + y };
                let mymacro = macro(x, y) { x + y; };
                """);
        Environment env = new Environment();

        Program remaining = MacroExpansion.defineMacro(program, env);

        assertEquals(2, remaining.statements().size(), "remaining statements");
        // defining macros does not evaluate anything else
        assertEquals(Optional.empty(), env.get("number"));
        assertEquals(Optional.empty(), env.get("function"));

        MacroObject macro = assertInstanceOf(MacroObject.class, env.get("mymacro").orElseThrow());
        assertEquals(2, macro.params().size(), "params size");
        assertEquals("x", macro.params().get(0).toString());
        assertEquals("y", macro.params().get(1).toString());
        assertEquals("(x + y)", macro.body().toString());
    }

    @Test
    void testDefineMacrosLeavesOriginalProgramUnchanged() {
        Program program = parse("let mymacro = macro(x) { x }; 1;");

        MacroExpansion.defineMacro(program, new Environment());

        assertEquals(2, program.statements().size(), "original statements");
    }

    static Stream<Arguments> macroPrograms() {
        return Stream.of(
                arguments("""
                        let infixExpression = macro() { quote(1 + 2); };
                        infixExpression();
                        """,
                        "(1 + 2)"),
                arguments("""
                        let reverse = macro(a, b) { quote(unquote(b) - unquote(a)); };
                        reverse(2 + 2, 10 - 5);
                        """,
                        "(10 - 5) - (2 + 2)"),
                arguments("""
                        let unless = macro(condition, consequence, alternative) {
                            quote(if (!(unquote(condition))) {
                                unquote(consequence);
                            } else {
                                unquote(alternative);
                            });
                        };

                        unless(10 > 5, puts("not greater"), puts("greater"));
                        """,
                        "if (!(10 > 5)) { puts(\"not greater\") } else { puts(\"greater\") }"),
                // not in WAIIG: one macro expanded twice must not leak the first call's arguments into the second
                arguments("""
                        let reverse = macro(a, b) { quote(unquote(b) - unquote(a)); };
                        reverse(1, 2);
                        reverse(3, 4);
                        """,
                        "(2 - 1); (4 - 3)"),
                // not in WAIIG: a macro call inside the arguments of an ordinary call
                arguments("""
                        let double = macro(x) { quote(unquote(x) * 2); };
                        puts(double(3));
                        """,
                        "puts((3 * 2))"),
                // not in WAIIG: macro arguments are passed as code, so an unbound name is not an error
                arguments("""
                        let ignore = macro(x) { quote(1); };
                        ignore(notDefinedAnywhere);
                        """,
                        "1")
        );
    }

    @ParameterizedTest
    @MethodSource("macroPrograms")
    void testExpandMacros(String input, String expected) {
        Program program = parse(input);
        Environment env = new Environment();

        Program expanded = MacroExpansion.expandMacros(MacroExpansion.defineMacro(program, env), env);

        assertEquals(parse(expected).toString(), expanded.toString());
    }

    static Stream<Arguments> failingMacroPrograms() {
        return Stream.of(
                arguments("let m = macro() { 1 }; m();",
                        "cannot expand macro m: macro must return quoted code, got INTEGER"),
                arguments("let m = macro() { }; m();",
                        "cannot expand macro m: macro must return quoted code, got nothing"),
                // unquote only means something inside quote; elsewhere it is a call to an undefined function
                arguments("let m = macro(x) { !unquote(x) }; m(true);",
                        "cannot expand macro m: identifier not found: unquote"),
                // code that runs during expansion cannot call another macro like a function
                arguments("""
                        let reverse = macro(val) { quote(!unquote(val)) };
                        let m = macro(x) { quote(unquote(reverse(x))) };
                        m(true);
                        """,
                        "cannot expand macro m: not a function: MACRO"),
                arguments("let m = macro(a, b) { quote(1) }; m(1);",
                        "cannot expand macro m: wrong number of arguments. got=1, want=2")
        );
    }

    @ParameterizedTest
    @MethodSource("failingMacroPrograms")
    void testExpansionFailureNamesMacroAndReason(String input, String expectedMessage) {
        Environment env = new Environment();
        Program program = MacroExpansion.defineMacro(parse(input), env);

        RuntimeException failure = assertThrows(RuntimeException.class,
                () -> MacroExpansion.expandMacros(program, env));
        assertEquals(expectedMessage, failure.getMessage());
    }

    @Test
    void testExpandedProgramEvaluates() {
        String unless = """
                let unless = macro(condition, consequence, alternative) {
                    quote(if (!(unquote(condition))) {
                        unquote(consequence);
                    } else {
                        unquote(alternative);
                    });
                };
                """;

        assertEquals("2", evalWithMacros(unless + "unless(10 > 5, 1, 2);").inspect());
        assertEquals("1", evalWithMacros(unless + "unless(1 > 5, 1, 2);").inspect());
    }

    private static MonkeyObject evalWithMacros(String input) {
        Environment macroEnv = new Environment();
        Program expanded = MacroExpansion.expandMacros(MacroExpansion.defineMacro(parse(input), macroEnv), macroEnv);
        return Evaluator.eval(expanded, new Environment());
    }

    private static Program parse(String input) {
        Parser parser = new Parser(new Lexer(input));
        Program program = parser.parseProgram();
        assertEquals(List.of(), parser.errors(), "parser errors");
        return program;
    }
}
