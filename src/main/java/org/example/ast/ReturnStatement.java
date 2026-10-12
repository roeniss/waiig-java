package org.example.ast;

import org.example.token.Token;

import java.util.Objects;

public record ReturnStatement(Token token, Expression value) implements Statement {
    @Override
    public String toString() {
        return tokenLiteral() + " " + Objects.toString(value, "") + ";";
    }
}
