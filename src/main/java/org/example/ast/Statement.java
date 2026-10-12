package org.example.ast;

import org.example.token.Token;

public interface Statement extends Node {
    Token token();

    @Override
    default String tokenLiteral() {
        return token().literal();
    }
}
