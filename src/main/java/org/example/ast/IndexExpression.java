package org.example.ast;

import org.example.token.Token;

public record IndexExpression(Token token, Expression left, Expression index) implements Expression {
    @Override
    public String toString() {
        return "(%s[%s])".formatted(left, index);
    }
}
