package org.example.ast;

import org.example.token.Token;

public record PrefixExpression(Token token, String operator, Expression right) implements Expression {
    @Override
    public String toString() {
        return "(" + operator + right + ")";
    }
}
