package org.example.ast;

import org.example.token.Token;

public sealed interface Statement extends Node
        permits LetStatement, ReturnStatement, ExpressionStatement, BlockStatement {
    Token token();

    @Override
    default String tokenLiteral() {
        return token().literal();
    }
}
