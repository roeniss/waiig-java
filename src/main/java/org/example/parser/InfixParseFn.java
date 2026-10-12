package org.example.parser;

import org.example.ast.Expression;

@FunctionalInterface
public interface InfixParseFn {
    Expression parse(Expression left);
}
