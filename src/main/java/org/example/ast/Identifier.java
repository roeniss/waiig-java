package org.example.ast;

import org.example.token.Token;

public record Identifier(Token token, String value) implements Expression {
    @Override
    public String toString() {
        return value;
    }
}