package org.example.object;

import java.util.List;
import java.util.stream.Collectors;

public record ArrayObject(List<MonkeyObject> elements) implements MonkeyObject {
    @Override
    public ObjectType type() {
        return ObjectType.ARRAY;
    }

    @Override
    public String inspect() {
        return elements.stream()
                .map(MonkeyObject::inspect)
                .collect(Collectors.joining(", ", "[", "]"));
    }
}
