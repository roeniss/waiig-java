package org.example.object;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class EnvironmentTest {

    @Test
    void testGetReturnsWhatWasSet() {
        Environment env = new Environment();
        IntegerObject five = new IntegerObject(5);

        env.set("x", five);

        assertSame(five, env.get("x").orElseThrow());
    }

    @Test
    void testGetMissingNameIsEmpty() {
        Environment env = new Environment();

        assertEquals(Optional.empty(), env.get("x"));
    }

    @Test
    void testSetOverwritesPreviousValue() {
        Environment env = new Environment();

        env.set("x", new IntegerObject(5));
        env.set("x", BooleanObject.TRUE);

        assertSame(BooleanObject.TRUE, env.get("x").orElseThrow());
    }

    @Test
    void testSetReturnsTheValue() {
        Environment env = new Environment();
        IntegerObject five = new IntegerObject(5);

        assertSame(five, env.set("x", five));
    }

    @Test
    void testGetFallsBackToOuter() {
        Environment outer = new Environment();
        IntegerObject five = new IntegerObject(5);
        outer.set("x", five);

        Environment inner = new Environment(outer);

        assertSame(five, inner.get("x").orElseThrow());
    }

    @Test
    void testInnerBindingShadowsOuterWithoutChangingIt() {
        Environment outer = new Environment();
        outer.set("x", new IntegerObject(10));
        Environment inner = new Environment(outer);

        inner.set("x", new IntegerObject(1));

        assertEquals(new IntegerObject(1), inner.get("x").orElseThrow());
        assertEquals(new IntegerObject(10), outer.get("x").orElseThrow());
    }

    @Test
    void testInnerBindingIsNotVisibleFromOuter() {
        Environment outer = new Environment();
        Environment inner = new Environment(outer);

        inner.set("y", new IntegerObject(1));

        assertEquals(Optional.empty(), outer.get("y"));
    }

    @Test
    void testOuterChangesAfterCreationAreVisible() {
        // the inner environment holds a reference to outer, not a copy; recursive functions rely on this
        Environment outer = new Environment();
        Environment inner = new Environment(outer);

        outer.set("x", new IntegerObject(5));

        assertEquals(new IntegerObject(5), inner.get("x").orElseThrow());
    }

    @Test
    void testMissingEverywhereIsEmpty() {
        Environment outer = new Environment();
        Environment inner = new Environment(new Environment(outer));

        assertEquals(Optional.empty(), inner.get("x"));
    }
}
