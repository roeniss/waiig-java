package org.example.ast;

import java.util.List;
import java.util.stream.Collectors;

// a.k.a. Root Node
public record Program(List<Statement> statements) implements Node {
    @Override
    public String tokenLiteral() {
        if (statements != null && !statements.isEmpty()) {
            return statements.getFirst().tokenLiteral();
        } else {
            return "";
        }
    }

    @Override
    public String toString() {
        return statements.stream()
                .map(Statement::toString)
                .collect(Collectors.joining());
    }
}
