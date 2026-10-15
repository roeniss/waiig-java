package org.example.object;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StringObjectTest {

    @Test
    void testStringObject() {
        StringObject object = new StringObject("hello world");

        assertEquals("hello world", object.value());
        assertEquals(ObjectType.STRING, object.type());
        assertEquals("hello world", object.inspect());
    }
}
