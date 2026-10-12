package org.example.parser;

import org.example.ast.Expression;

@FunctionalInterface
public interface PrefixParseFn {
    Expression parse();
}
