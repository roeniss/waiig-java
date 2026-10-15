package org.example.ast;

import org.example.token.Token;

public sealed interface Expression extends Node
    permits ArrayLiteral, BooleanLiteral, CallExpression, FunctionLiteral, HashLiteral, Identifier, IfExpression, IndexExpression,
    InfixExpression, NumberLiteral, PrefixExpression, StringLiteral {
    Token token();

    @Override
    default String tokenLiteral() {
        return token().literal();
    }
}
