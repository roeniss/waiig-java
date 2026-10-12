package org.example.ast;

import org.example.token.Token;

public record BooleanLiteral(Token token, boolean value) implements Expression {
    @Override
    public String toString() {
        return token.literal();
    }
}
