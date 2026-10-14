package org.example.object;

public record ErrorObject(String message) implements MonkeyObject {

    @Override
    public ObjectType type() {
        return ObjectType.ERROR;
    }

    @Override
    public String inspect() {
        return "ERROR: " + message;
    }
}
