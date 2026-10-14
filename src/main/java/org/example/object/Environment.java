package org.example.object;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Environment {

    private final Map<String, MonkeyObject> store = new HashMap<>();
    private final Environment outer;

    public Environment() {
        this(null);
    }

    // used for a function call: names not bound here are looked up in outer, but set() only ever writes here
    public Environment(Environment outer) {
        this.outer = outer;
    }

    public Optional<MonkeyObject> get(String name) {
        MonkeyObject value = store.get(name);
        if (value == null && outer != null) {
            return outer.get(name);
        }
        return Optional.ofNullable(value);
    }

    public MonkeyObject set(String name, MonkeyObject value) {
        store.put(name, value);
        return value;
    }
}
