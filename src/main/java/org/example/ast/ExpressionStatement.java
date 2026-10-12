package org.example.ast;

import org.example.token.Token;

import java.util.Objects;

public record ExpressionStatement(Token token, Expression expression) implements Statement {
    @Override
    public String toString() {
        return Objects.toString(expression, "");
    }
}
