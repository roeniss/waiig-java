package org.example.object;

public record BuiltinObject(BuiltinFunction function) implements MonkeyObject {

    @Override
    public ObjectType type() {
        return ObjectType.BUILTIN;
    }

    @Override
    public String inspect() {
        return "builtin function";
    }
}
