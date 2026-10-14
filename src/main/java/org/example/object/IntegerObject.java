package org.example.object;

public record IntegerObject(long value) implements MonkeyObject {
    @Override
    public ObjectType type() {
        return ObjectType.INTEGER;
    }

    @Override
    public String inspect() {
        return String.valueOf(value);
    }
}
