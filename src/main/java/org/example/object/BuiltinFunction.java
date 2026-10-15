package org.example.object;

import java.util.List;

@FunctionalInterface
public interface BuiltinFunction {
    MonkeyObject apply(List<MonkeyObject> arguments);
}
