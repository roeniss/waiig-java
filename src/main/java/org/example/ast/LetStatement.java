package org.example.ast;

import org.example.token.Token;

import java.util.Objects;

public record LetStatement(Token token, Identifier name, Expression value) implements Statement {
    @Override
    public String toString() {
        return tokenLiteral() + " " + name + " = " + Objects.toString(value, "") + ";";
    }
}
