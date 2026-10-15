package org.example.object;

public sealed interface Hashable
    permits IntegerObject, BooleanObject, StringObject {
       HashKey hashKey();
}
