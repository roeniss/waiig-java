package org.example.object;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class Builtins {
    private static final Map<String, BuiltinObject> BUILTINS = Map.of(
        "len", new BuiltinObject(Builtins::len),
        "first", new BuiltinObject(Builtins::first),
        "last", new BuiltinObject(Builtins::last),
        "rest", new BuiltinObject(Builtins::rest),
        "push", new BuiltinObject(Builtins::push),
        "puts", new BuiltinObject(Builtins::puts)
    );

    // where puts writes; null means System.out, looked up at call time so System.setOut still applies
    private static PrintStream output;

    private Builtins() {
    }

    public static Optional<BuiltinObject> get(String name) {
        return Optional.ofNullable(BUILTINS.get(name));
    }

    public static PrintStream output() {
        return output;
    }

    public static void setOutput(PrintStream newOutput) {
        output = newOutput;
    }

    private static MonkeyObject puts(List<MonkeyObject> arguments) {
        PrintStream out = output != null ? output : System.out;
        for (MonkeyObject argument : arguments) {
            out.println(argument.inspect());
        }
        return NullObject.NULL;
    }

    private static MonkeyObject push(List<MonkeyObject> arguments) {
        if (arguments.size() != 2) {
            return new ErrorObject(
                "wrong number of arguments. got=%d, want=2"
                    .formatted(arguments.size())
            );
        }

        return switch (arguments.getFirst()) {
            case ArrayObject(List<MonkeyObject> elements) -> {
                List<MonkeyObject> newElements =
                    new ArrayList<>(elements);
                newElements.add(arguments.getLast());

                yield new ArrayObject(List.copyOf(newElements));

            }
            default -> new ErrorObject(
                "argument to `push` must be ARRAY, got " +
                    arguments.getFirst().type()
            );
        };
    }

    private static MonkeyObject rest(List<MonkeyObject> arguments) {
        if (arguments.size() != 1) {
            return new ErrorObject(
                "wrong number of arguments. got=%d, want=1"
                    .formatted(arguments.size())
            );
        }

        return switch (arguments.getFirst()) {
            case ArrayObject(List<MonkeyObject> elements) -> elements.isEmpty()
                ? NullObject.NULL
                : new ArrayObject(List.copyOf(elements.subList(1, elements.size())));
            default -> new ErrorObject(
                "argument to `rest` must be ARRAY, got " +
                    arguments.getFirst().type()
            );
        };
    }

    private static MonkeyObject last(List<MonkeyObject> arguments) {
        if (arguments.size() != 1) {
            return new ErrorObject(
                "wrong number of arguments. got=%d, want=1"
                    .formatted(arguments.size())
            );
        }

        return switch (arguments.getFirst()) {
            case ArrayObject(List<MonkeyObject> elements) -> elements.isEmpty() ? NullObject.NULL : elements.getLast();
            default -> new ErrorObject(
                "argument to `last` must be ARRAY, got " +
                    arguments.getFirst().type()
            );
        };
    }

    private static MonkeyObject first(List<MonkeyObject> arguments) {
        if (arguments.size() != 1) {
            return new ErrorObject(
                "wrong number of arguments. got=%d, want=1"
                    .formatted(arguments.size())
            );
        }

        return switch (arguments.getFirst()) {
            case ArrayObject(List<MonkeyObject> elements) -> elements.isEmpty() ? NullObject.NULL : elements.getFirst();
            default -> new ErrorObject(
                "argument to `first` must be ARRAY, got " +
                    arguments.getFirst().type()
            );
        };
    }

    private static MonkeyObject len(List<MonkeyObject> arguments) {
        if (arguments.size() != 1) {
            return new ErrorObject(
                "wrong number of arguments. got=%d, want=1"
                    .formatted(arguments.size())
            );
        }

        return switch (arguments.getFirst()) {
            // count characters, not UTF-16 units: an emoji is one character but two Java chars
            case StringObject(String value) -> new IntegerObject(value.codePointCount(0, value.length()));
            case ArrayObject(List<MonkeyObject> elements) -> new IntegerObject(elements.size());
            default -> new ErrorObject(
                "argument to `len` not supported, got " +
                    arguments.getFirst().type()
            );
        };
    }
}

