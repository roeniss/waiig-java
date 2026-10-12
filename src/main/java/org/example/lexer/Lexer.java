package org.example.lexer;

import org.example.token.Token;
import org.example.token.TokenType;

public class Lexer {

    private final String input;
    private int pos = -1; // pointing current char
    private char ch;


    public Lexer(String input) {
        this.input = input;
        readChar();
    }

    public Token nextToken() {
        Token token;

        skipWhitespace();

        switch (ch) {
            case '\0' -> token = new Token(TokenType.EOF, "");
            case '+' -> token = new Token(TokenType.PLUS, String.valueOf(ch));
            case '-' -> token = new Token(TokenType.MINUS, String.valueOf(ch));
            case '*' -> token = new Token(TokenType.ASTERISK, String.valueOf(ch));
            case '/' -> token = new Token(TokenType.SLASH, String.valueOf(ch));
            case '<' -> token = new Token(TokenType.LT, String.valueOf(ch));
            case '>' -> token = new Token(TokenType.GT, String.valueOf(ch));
            case ',' -> token = new Token(TokenType.COMMA, String.valueOf(ch));
            case ';' -> token = new Token(TokenType.SEMICOLON, String.valueOf(ch));
            case '(' -> token = new Token(TokenType.LPAREN, String.valueOf(ch));
            case ')' -> token = new Token(TokenType.RPAREN, String.valueOf(ch));
            case '{' -> token = new Token(TokenType.LBRACE, String.valueOf(ch));
            case '}' -> token = new Token(TokenType.RBRACE, String.valueOf(ch));
            case '=' -> {
                if (peekChar() == '=') {
                    readChar();
                    token = new Token(TokenType.EQ, "==");
                } else {
                    token = new Token(TokenType.ASSIGN, String.valueOf(ch));
                }
            }
            case '!' -> {
                if (peekChar() == '=') {
                    readChar();
                    token = new Token(TokenType.NEQ, "!=");
                } else {
                    token = new Token(TokenType.BANG, String.valueOf(ch));
                }
            }
            default -> {
                if (Character.isLetter(ch)) {
                    String identifier = readIdentifier();
                    return new Token(TokenType.lookupKeyword(identifier), identifier);
                } else if (Character.isDigit(ch)) {
                    return new Token(TokenType.NUMBER, readNumber());
                } else {
                    token = new Token(TokenType.ILLEGAL, String.valueOf(ch));
                }
            }
        }

        readChar();
        return token;
    }

    private String readNumber() {
        int start = pos;
        while (Character.isDigit(ch)) {
            readChar();
        }
        return input.substring(start, pos);
    }

    private void skipWhitespace() {
        while (Character.isWhitespace(ch)) {
            readChar();
        }
    }

    private String readIdentifier() {
        int start = pos;
        while (Character.isLetter(ch) || ch == '_') {
            readChar();
        }
        return input.substring(start, pos);
    }

    private void readChar() {
        pos++;
        if (pos >= input.length()) {
            ch = '\0';
        } else {
            ch = input.charAt(pos);
        }
    }

    private char peekChar() {
        int nextPos = pos + 1;
        if (nextPos >= input.length()) {
            return '\0';
        } else {
            return input.charAt(nextPos);
        }
    }

}
