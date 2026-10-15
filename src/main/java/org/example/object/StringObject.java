package org.example.object;

import java.nio.charset.StandardCharsets;

public record StringObject(String value) implements MonkeyObject, Hashable {
    @Override
    public ObjectType type() {
        return ObjectType.STRING;
    }

    @Override
    public String inspect() {
        return value;
    }

    private static final long FNV_OFFSET_BASIS =
        0xcbf29ce484222325L;
    private static final long FNV_PRIME =
        0x100000001b3L;

    @Override
    public HashKey hashKey() {
        long hash = FNV_OFFSET_BASIS;

        for (byte current :
            value.getBytes(StandardCharsets.UTF_8)) {
            hash ^= current & 0xffL;
            hash *= FNV_PRIME;
        }

        return new HashKey(ObjectType.STRING, hash);
    }

}

