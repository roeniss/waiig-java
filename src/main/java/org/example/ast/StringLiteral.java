package org.example.ast;

import org.example.token.Token;

public record StringLiteral(Token token, String value) implements Expression {
    @Override
    public String toString() {
        return token.literal();
    }
}
