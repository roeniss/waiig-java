package org.example.object;

public sealed interface MonkeyObject
    permits ArrayObject, BooleanObject, BuiltinObject, ErrorObject, FunctionObject, HashObject, IntegerObject, NullObject, ReturnObject, StringObject {
    ObjectType type();

    String inspect();
}
