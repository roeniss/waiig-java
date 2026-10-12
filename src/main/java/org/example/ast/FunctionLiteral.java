package org.example.ast;

import org.example.token.Token;

import java.util.List;
import java.util.stream.Collectors;

public record FunctionLiteral(Token token, List<Identifier> parameters, BlockStatement body) implements Expression {
    @Override
    public String toString() {
        // parameters can be null after a parse error, like a nil slice in the book
        String params = parameters == null ? "" : parameters.stream()
                .map(Identifier::toString)
                .collect(Collectors.joining(", "));
        return tokenLiteral() + "(" + params + ") " + body;
    }
}
