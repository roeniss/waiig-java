package org.example.ast;

public sealed interface Node permits Program, Statement, Expression {
    String tokenLiteral();
    String toString();
}
