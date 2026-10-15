package org.example.ast;

import org.example.token.Token;

import java.util.List;
import java.util.stream.Collectors;

public record ArrayLiteral(Token token, List<Expression> elements) implements Expression {
    @Override
    public String toString() {
        return elements.stream()
                .map(Expression::toString)
                .collect(Collectors.joining(", ", "[", "]"));
    }
}
