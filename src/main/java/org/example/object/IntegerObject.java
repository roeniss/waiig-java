package org.example.object;

public record IntegerObject(long value) implements MonkeyObject, Hashable {
    @Override
    public ObjectType type() {
        return ObjectType.INTEGER;
    }

    @Override
    public String inspect() {
        return String.valueOf(value);
    }

    @Override
    public HashKey hashKey() {
        return new HashKey(ObjectType.INTEGER, value);
    }
}
