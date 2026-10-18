package org.example.repl;

import org.example.object.Builtins;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReplTest {

    @Test
    void testPrintsEvaluatedResult() throws IOException {
        String output = run("""
                5
                10
                """);

        assertEquals("""
                >> 5
                >> 10
                >>\s""", output);
    }

    @Test
    void testPrintsNothingForEmptyLine() throws IOException {
        String output = run("\n5\n");

        assertEquals(">> >> 5\n>> ", output);
    }

    @Test
    void testPrintsParserErrors() throws IOException {
        String output = run("let x 12 * 3;\n");

        assertTrue(output.contains("Woops! We ran into some monkey business here!"), output);
        assertTrue(output.contains("\texpected next token to be ASSIGN, got NUMBER instead\n"), output);
    }

    @Test
    void testKeepsRunningAfterParserError() throws IOException {
        String output = run("""
                let x 12 * 3;
                5
                """);

        assertTrue(output.endsWith(">> 5\n>> "), output);
    }

    @Test
    void testRemembersBindingsAcrossLines() throws IOException {
        String output = run("""
                let x = 5
                x * 2
                """);

        assertEquals(">> >> 10\n>> ", output);
    }

    @Test
    void testPutsWritesToReplOutput() throws IOException {
        String output = run("puts(\"Hello!\")\n");

        // puts prints its argument, then the REPL prints the NULL that puts returns
        assertEquals(">> Hello!\nnull\n>> ", output);
    }

    @Test
    void testKeepsRunningAfterUnquotingAValueWithNoSourceForm() throws IOException {
        String output = run("""
                quote(unquote(fn(x) { x }))
                5
                """);

        assertEquals(">> ERROR: cannot unquote FUNCTION\n>> 5\n>> ", output);
    }

    @Test
    void testMacroDefinedOnOneLineExpandsOnTheNext() throws IOException {
        String output = run("""
                let unless = macro(condition, consequence, alternative) { quote(if (!(unquote(condition))) { unquote(consequence); } else { unquote(alternative); }); };
                unless(10 > 5, puts("not greater"), puts("greater"));
                """);

        // the definition line prints nothing; only the branch unless picked runs its puts
        assertEquals(">> >> greater\nnull\n>> ", output);
    }

    @Test
    void testKeepsRunningAfterMacroExpansionFails() throws IOException {
        // a macro must return quoted code, so this one cannot be expanded
        String output = run("""
                let broken = macro() { 1 };
                broken();
                5
                """);

        assertTrue(output.contains("evaluation failed: "), output);
        assertTrue(output.endsWith(">> 5\n>> "), output);
    }

    @Test
    void testRestoresPutsOutputWhenSessionEnds() throws IOException {
        PrintStream before = Builtins.output();

        run("puts(1)\n");

        assertSame(before, Builtins.output());
    }

    private static String run(String input) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        Repl.start(
                new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)),
                new PrintStream(buffer, true, StandardCharsets.UTF_8));
        return buffer.toString(StandardCharsets.UTF_8);
    }
}
