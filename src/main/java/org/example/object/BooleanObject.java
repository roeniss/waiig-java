package org.example.object;

public record BooleanObject(boolean value) implements MonkeyObject {
    @Override
    public ObjectType type() {
        return ObjectType.BOOLEAN;
    }

    @Override
    public String inspect() {
        return String.valueOf(value);
    }

    public static final BooleanObject TRUE = new BooleanObject(true);
    public static final BooleanObject FALSE = new BooleanObject(false);
}
