package org.example.ast;

import org.example.token.Token;

import java.util.Map;
import java.util.stream.Collectors;

public record HashLiteral(Token token, Map<Expression, Expression> pairs) implements Expression {
    @Override
    public String toString() {
        return pairs.entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining(", ", "{", "}"));
    }
}
