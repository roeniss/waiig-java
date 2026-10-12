package org.example.parser;

import org.example.token.TokenType;

import java.util.Map;

public enum Precedence {
    LOWEST,
    EQUALS,
    LESSGREATER,
    SUM,
    PRODUCT,
    PREFIX,
    CALL,
    ;

    private static final Map<TokenType, Precedence> precedence = Map.of(
            TokenType.EQ, EQUALS,
            TokenType.NEQ, EQUALS,
            TokenType.LT, LESSGREATER,
            TokenType.GT, LESSGREATER,
            TokenType.PLUS, SUM,
            TokenType.MINUS, SUM,
            TokenType.SLASH, PRODUCT,
            TokenType.ASTERISK, PRODUCT,
            TokenType.LPAREN, CALL
    );

    public static Precedence getPrecedence(TokenType tokenType) {
        return precedence.getOrDefault(tokenType, LOWEST);
    }

    public boolean isLowerThan(Precedence other) {
        return compareTo(other) < 0;
    }
}
