package org.example.object;

public record ReturnObject(MonkeyObject value) implements MonkeyObject {

    @Override
    public ObjectType type() {
        return ObjectType.RETURN;
    }

    @Override
    public String inspect() {
        return value.inspect();
    }
}
