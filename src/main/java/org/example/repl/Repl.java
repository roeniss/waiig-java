package org.example.repl;

import org.example.lexer.Lexer;
import org.example.token.Token;
import org.example.token.TokenType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;

public class Repl {

    private static final String PROMPT = ">> ";

    public static void start(InputStream in, PrintStream out) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));

        while (true) {
            out.print(PROMPT);
            out.flush();

            String line = reader.readLine();
            if (line == null) {
                return;
            }

            Lexer lexer = new Lexer(line);
            for (Token token = lexer.nextToken(); token.type() != TokenType.EOF; token = lexer.nextToken()) {
                out.println(token);
            }
        }
    }
}
