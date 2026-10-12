package org.example.ast;

import org.example.token.Token;

public record NumberLiteral(Token token, long value) implements Expression {
    @Override
    public String toString() {
        return token.literal();
    }
}
