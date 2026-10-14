package org.example.repl;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    private static String run(String input) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        Repl.start(
                new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)),
                new PrintStream(buffer, true, StandardCharsets.UTF_8));
        return buffer.toString(StandardCharsets.UTF_8);
    }
}
