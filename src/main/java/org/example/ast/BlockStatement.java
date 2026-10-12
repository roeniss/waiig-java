package org.example.ast;

import org.example.token.Token;

import java.util.List;
import java.util.stream.Collectors;

public record BlockStatement(Token token, List<Statement> statements) implements Statement {
    @Override
    public String toString() {
        return statements.stream()
                .map(Statement::toString)
                .collect(Collectors.joining());
    }
}
