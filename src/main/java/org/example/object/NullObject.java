package org.example.object;

public record NullObject() implements MonkeyObject {

    @Override
    public ObjectType type() {
        return ObjectType.NULL;
    }

    @Override
    public String inspect() {
        return "null";
    }

    public static final NullObject NULL = new NullObject();
}
