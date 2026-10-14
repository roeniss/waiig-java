package org.example.ast;

import org.example.token.Token;

public sealed interface Expression extends Node
        permits Identifier, NumberLiteral, BooleanLiteral, PrefixExpression, InfixExpression,
        IfExpression, FunctionLiteral, CallExpression {
    Token token();

    @Override
    default String tokenLiteral() {
        return token().literal();
    }
}
