package org.example.object;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class HashKeyTest {

    @Test
    void testIntegerHashKeys() {
        assertEquals(new IntegerObject(1).hashKey(), new IntegerObject(1).hashKey());
        assertNotEquals(new IntegerObject(1).hashKey(), new IntegerObject(2).hashKey());
        assertNotEquals(new IntegerObject(0).hashKey(), new IntegerObject(1L << 32).hashKey());
    }

    @Test
    void testBooleanHashKeys() {
        assertEquals(BooleanObject.TRUE.hashKey(), BooleanObject.TRUE.hashKey());
        assertNotEquals(BooleanObject.TRUE.hashKey(), BooleanObject.FALSE.hashKey());
    }

    @Test
    void testHashKeysIncludeObjectType() {
        assertNotEquals(new IntegerObject(1).hashKey(), BooleanObject.TRUE.hashKey());
    }

    @Test
    void testStringHashKeys() {
        assertEquals(new StringObject("hello").hashKey(), new StringObject("hello").hashKey());
        assertNotEquals(new StringObject("hello").hashKey(), new StringObject("world").hashKey());
        assertNotEquals(new StringObject("FB").hashKey(), new StringObject("Ea").hashKey());
    }
}
