package org.example.object;

import java.util.Map;
import java.util.stream.Collectors;

public record HashObject(Map<HashKey, HashPair> pairs) implements MonkeyObject {

    @Override
    public ObjectType type() {
        return ObjectType.HASH;
    }

    @Override
    public String inspect() {
        return pairs.values().stream()
            .map(pair ->
                pair.key().inspect()
                    + ": "
                    + pair.value().inspect()
            )
            .collect(Collectors.joining(", ", "{", "}"));
    }

}
