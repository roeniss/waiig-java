package org.example.lexer;

import org.example.token.Token;
import org.example.token.TokenType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class LexerTest {

    @Test
    void testSingleCharTokens() {
        String input = "=+(){},;-!*/<>";
        List<Token> expected = List.of(
                new Token(TokenType.ASSIGN, "="),
                new Token(TokenType.PLUS, "+"),
                new Token(TokenType.LPAREN, "("),
                new Token(TokenType.RPAREN, ")"),
                new Token(TokenType.LBRACE, "{"),
                new Token(TokenType.RBRACE, "}"),
                new Token(TokenType.COMMA, ","),
                new Token(TokenType.SEMICOLON, ";"),
                new Token(TokenType.MINUS, "-"),
                new Token(TokenType.BANG, "!"),
                new Token(TokenType.ASTERISK, "*"),
                new Token(TokenType.SLASH, "/"),
                new Token(TokenType.LT, "<"),
                new Token(TokenType.GT, ">"),
                new Token(TokenType.EOF, "")
        );

        assertTokens(input, expected);
    }

    @Test
    void testKeywords() {
        // the last three look like keywords but must be identifiers
        String input = "fn let true false if else return lets iffy True";
        List<Token> expected = List.of(
                new Token(TokenType.FUNCTION, "fn"),
                new Token(TokenType.LET, "let"),
                new Token(TokenType.TRUE, "true"),
                new Token(TokenType.FALSE, "false"),
                new Token(TokenType.IF, "if"),
                new Token(TokenType.ELSE, "else"),
                new Token(TokenType.RETURN, "return"),
                new Token(TokenType.IDENTIFIER, "lets"),
                new Token(TokenType.IDENTIFIER, "iffy"),
                new Token(TokenType.IDENTIFIER, "True"),
                new Token(TokenType.EOF, "")
        );

        assertTokens(input, expected);
    }

    @Test
    void testTwoCharTokens() {
        String input = "== != = = === !== =! =";
        List<Token> expected = List.of(
                new Token(TokenType.EQ, "=="),
                new Token(TokenType.NEQ, "!="),
                // "= =": a space splits them
                new Token(TokenType.ASSIGN, "="),
                new Token(TokenType.ASSIGN, "="),
                // "===": the first two pair up
                new Token(TokenType.EQ, "=="),
                new Token(TokenType.ASSIGN, "="),
                // "!=="
                new Token(TokenType.NEQ, "!="),
                new Token(TokenType.ASSIGN, "="),
                // "=!": not a two-char token
                new Token(TokenType.ASSIGN, "="),
                new Token(TokenType.BANG, "!"),
                // "=" as the last char: peeking past the end must not throw
                new Token(TokenType.ASSIGN, "="),
                new Token(TokenType.EOF, "")
        );

        assertTokens(input, expected);
    }

    @Test
    void testStringTokens() {
        String input = "\"foobar\" \"foo bar\"";
        List<Token> expected = List.of(
                new Token(TokenType.STRING, "foobar"),
                new Token(TokenType.STRING, "foo bar"),
                new Token(TokenType.EOF, "")
        );

        assertTokens(input, expected);
    }

    @Test
    void testArrayTokens() {
        String input = "[1, 2];";
        List<Token> expected = List.of(
                new Token(TokenType.LBRACKET, "["),
                new Token(TokenType.NUMBER, "1"),
                new Token(TokenType.COMMA, ","),
                new Token(TokenType.NUMBER, "2"),
                new Token(TokenType.RBRACKET, "]"),
                new Token(TokenType.SEMICOLON, ";"),
                new Token(TokenType.EOF, "")
        );

        assertTokens(input, expected);
    }

    @Test
    void testHashTokens() {
        String input = "{\"one\": 1, \"two\": 2}";
        List<Token> expected = List.of(
                new Token(TokenType.LBRACE, "{"),
                new Token(TokenType.STRING, "one"),
                new Token(TokenType.COLON, ":"),
                new Token(TokenType.NUMBER, "1"),
                new Token(TokenType.COMMA, ","),
                new Token(TokenType.STRING, "two"),
                new Token(TokenType.COLON, ":"),
                new Token(TokenType.NUMBER, "2"),
                new Token(TokenType.RBRACE, "}"),
                new Token(TokenType.EOF, "")
        );

        assertTokens(input, expected);
    }

    @Test
    void testSimpleProgram() {
        String input = """
                let five = 5;
                let ten = 10;
                
                let add = fn(x, y) {
                  x + y;
                };
                
                let result = add(five, ten);
                !-/*5;
                5 < 10 > 5;

                if (5 < 10) {
                    return true;
                } else {
                    return false;
                }

                10 == 10;
                10 != 9;
                """;

        List<Token> expected = List.of(
                // let five = 5;
                new Token(TokenType.LET, "let"),
                new Token(TokenType.IDENTIFIER, "five"),
                new Token(TokenType.ASSIGN, "="),
                new Token(TokenType.NUMBER, "5"),
                new Token(TokenType.SEMICOLON, ";"),
                // let ten = 10;
                new Token(TokenType.LET, "let"),
                new Token(TokenType.IDENTIFIER, "ten"),
                new Token(TokenType.ASSIGN, "="),
                new Token(TokenType.NUMBER, "10"),
                new Token(TokenType.SEMICOLON, ";"),
                // let add = fn(x, y) {
                new Token(TokenType.LET, "let"),
                new Token(TokenType.IDENTIFIER, "add"),
                new Token(TokenType.ASSIGN, "="),
                new Token(TokenType.FUNCTION, "fn"),
                new Token(TokenType.LPAREN, "("),
                new Token(TokenType.IDENTIFIER, "x"),
                new Token(TokenType.COMMA, ","),
                new Token(TokenType.IDENTIFIER, "y"),
                new Token(TokenType.RPAREN, ")"),
                new Token(TokenType.LBRACE, "{"),
                //   x + y;
                new Token(TokenType.IDENTIFIER, "x"),
                new Token(TokenType.PLUS, "+"),
                new Token(TokenType.IDENTIFIER, "y"),
                new Token(TokenType.SEMICOLON, ";"),
                // };
                new Token(TokenType.RBRACE, "}"),
                new Token(TokenType.SEMICOLON, ";"),
                // let result = add(five, ten);
                new Token(TokenType.LET, "let"),
                new Token(TokenType.IDENTIFIER, "result"),
                new Token(TokenType.ASSIGN, "="),
                new Token(TokenType.IDENTIFIER, "add"),
                new Token(TokenType.LPAREN, "("),
                new Token(TokenType.IDENTIFIER, "five"),
                new Token(TokenType.COMMA, ","),
                new Token(TokenType.IDENTIFIER, "ten"),
                new Token(TokenType.RPAREN, ")"),
                new Token(TokenType.SEMICOLON, ";"),
                // !-/*5;
                new Token(TokenType.BANG, "!"),
                new Token(TokenType.MINUS, "-"),
                new Token(TokenType.SLASH, "/"),
                new Token(TokenType.ASTERISK, "*"),
                new Token(TokenType.NUMBER, "5"),
                new Token(TokenType.SEMICOLON, ";"),
                // 5 < 10 > 5;
                new Token(TokenType.NUMBER, "5"),
                new Token(TokenType.LT, "<"),
                new Token(TokenType.NUMBER, "10"),
                new Token(TokenType.GT, ">"),
                new Token(TokenType.NUMBER, "5"),
                new Token(TokenType.SEMICOLON, ";"),
                // if (5 < 10) {
                new Token(TokenType.IF, "if"),
                new Token(TokenType.LPAREN, "("),
                new Token(TokenType.NUMBER, "5"),
                new Token(TokenType.LT, "<"),
                new Token(TokenType.NUMBER, "10"),
                new Token(TokenType.RPAREN, ")"),
                new Token(TokenType.LBRACE, "{"),
                //     return true;
                new Token(TokenType.RETURN, "return"),
                new Token(TokenType.TRUE, "true"),
                new Token(TokenType.SEMICOLON, ";"),
                // } else {
                new Token(TokenType.RBRACE, "}"),
                new Token(TokenType.ELSE, "else"),
                new Token(TokenType.LBRACE, "{"),
                //     return false;
                new Token(TokenType.RETURN, "return"),
                new Token(TokenType.FALSE, "false"),
                new Token(TokenType.SEMICOLON, ";"),
                // }
                new Token(TokenType.RBRACE, "}"),
                // 10 == 10;
                new Token(TokenType.NUMBER, "10"),
                new Token(TokenType.EQ, "=="),
                new Token(TokenType.NUMBER, "10"),
                new Token(TokenType.SEMICOLON, ";"),
                // 10 != 9;
                new Token(TokenType.NUMBER, "10"),
                new Token(TokenType.NEQ, "!="),
                new Token(TokenType.NUMBER, "9"),
                new Token(TokenType.SEMICOLON, ";"),
                new Token(TokenType.EOF, "")
        );

        assertTokens(input, expected);
    }

    private static void assertTokens(String input, List<Token> expected) {
        Lexer l = new Lexer(input);

        List<Executable> assertions = new ArrayList<>();
        for (int i = 0; i < expected.size(); i++) {
            Token token = expected.get(i);
            String message = "tokens[" + i + "]";
            assertions.add(() -> assertEquals(token, l.nextToken(), message));
        }
        assertAll(assertions);
    }
}
