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
    void testPrintsParsedProgram() throws IOException {
        String output = run("""
                let x = 1 * 2 * 3 * 4 * 5
                x * y / 2 + 3 * 8 - 123
                true == false
                """);

        assertEquals("""
                >> let x = ((((1 * 2) * 3) * 4) * 5);
                >> ((((x * y) / 2) + (3 * 8)) - 123)
                >> (true == false)
                >>\s""", output);
    }

    @Test
    void testPrintsParserErrors() throws IOException {
        String output = run("let x 12 * 3;\n");

        assertTrue(output.contains("Woops! We ran into some monkey business here!"), output);
        assertTrue(output.contains("\texpected next token to be ASSIGN, got NUMBER instead\n"), output);
    }

    @Test
    void testKeepsRunningAfterError() throws IOException {
        String output = run("""
                let x 12 * 3;
                1 + 2
                """);

        assertTrue(output.endsWith(">> (1 + 2)\n>> "), output);
    }

    private static String run(String input) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        Repl.start(
                new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)),
                new PrintStream(buffer, true, StandardCharsets.UTF_8));
        return buffer.toString(StandardCharsets.UTF_8);
    }
}
