package org.example.ast;

import org.example.token.Token;

public record InfixExpression(Token token, Expression left, String operator, Expression right) implements Expression {
    @Override
    public String toString() {
        return "(" + left + " " + operator + " " + right + ")";
    }
}
