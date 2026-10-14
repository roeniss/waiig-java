package org.example.object;

public sealed interface MonkeyObject permits BooleanObject, ErrorObject, FunctionObject, IntegerObject, NullObject, ReturnObject {
    ObjectType type();

    String inspect();
}
