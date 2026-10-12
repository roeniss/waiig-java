package org.example.ast;

import org.example.token.Token;

public record IfExpression(Token token, Expression condition, BlockStatement consequence,
                           BlockStatement alternative) implements Expression {
    @Override
    public String toString() {
        String result = "if" + condition + " " + consequence;
        if (alternative != null) {
            result += "else " + alternative;
        }
        return result;
    }
}
