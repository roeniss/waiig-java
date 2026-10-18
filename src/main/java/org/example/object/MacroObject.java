package org.example.object;

import org.example.ast.BlockStatement;
import org.example.ast.Identifier;

import java.util.List;
import java.util.stream.Collectors;

public record MacroObject(List<Identifier> params, BlockStatement body, Environment env) implements MonkeyObject {

    @Override
    public ObjectType type() {
        return ObjectType.MACRO;
    }

    @Override
    public String inspect() {
        String parameters = params.stream()
                .map(Identifier::toString)
                .collect(Collectors.joining(", "));
        return "macro(" + parameters + ") {\n" + body + "\n}";
    }
}
