package org.example.object;

public record BooleanObject(boolean value) implements MonkeyObject, Hashable {
    @Override
    public ObjectType type() {
        return ObjectType.BOOLEAN;
    }

    @Override
    public String inspect() {
        return String.valueOf(value);
    }

    @Override
    public HashKey hashKey() {
         return new HashKey(ObjectType.BOOLEAN, value ? 1L : 0L);
    }

    public static final BooleanObject TRUE = new BooleanObject(true);
    public static final BooleanObject FALSE = new BooleanObject(false);
}
