package org.example.object;

import org.example.ast.Expression;

public record QuoteObject(Expression node) implements MonkeyObject {
    @Override
    public ObjectType type() {
        return ObjectType.QUOTE;
    }

    @Override
    public String inspect() {
        return "QUOTE(" + node.toString() + ")";
    }
}
