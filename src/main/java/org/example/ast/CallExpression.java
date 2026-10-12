package org.example.ast;

import org.example.token.Token;

import java.util.List;
import java.util.stream.Collectors;

// function is an Identifier (add(1, 2)) or a FunctionLiteral (fn(x) { x }(1))
public record CallExpression(Token token, Expression function, List<Expression> arguments) implements Expression {
    @Override
    public String toString() {
        // arguments can be null after a parse error, like a nil slice in the book
        String args = arguments == null ? "" : arguments.stream()
                .map(Expression::toString)
                .collect(Collectors.joining(", "));
        return function + "(" + args + ")";
    }
}
