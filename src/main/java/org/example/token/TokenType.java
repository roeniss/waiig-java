package org.example.token;

import java.util.Map;

public enum TokenType {
    ILLEGAL,
    EOF,
    IDENTIFIER,
    NUMBER,
    ASSIGN,
    PLUS,
    MINUS,
    BANG,
    ASTERISK,
    SLASH,
    LT,
    GT,
    EQ,
    NEQ,
    COMMA,
    COLON,
    SEMICOLON,
    LPAREN,
    RPAREN,
    LBRACE,
    RBRACE,
    FUNCTION,
    LET,
    TRUE,
    FALSE,
    IF,
    ELSE,
    RETURN,
    STRING,
    LBRACKET,
    RBRACKET,
    MACRO,
    ;

    private static final Map<String, TokenType> keywords = Map.of(
            "fn", FUNCTION,
            "let", LET,
            "true", TRUE,
            "false", FALSE,
            "if", IF,
            "else", ELSE,
            "return", RETURN,
            "macro", MACRO
    );

    public static TokenType lookupKeyword(String identifier) {
        return keywords.getOrDefault(identifier, IDENTIFIER);
    }
}

