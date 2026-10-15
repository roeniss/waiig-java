package org.example.evaluator;

import org.example.ast.Program;
import org.example.lexer.Lexer;
import org.example.object.*;
import org.example.parser.Parser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class EvaluatorTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            5                               | 5
            10                              | 10
            -5                              | -5
            -10                             | -10
            --5                             | 5
            5 + 5 + 5 + 5 - 10              | 10
            2 * 2 * 2 * 2 * 2               | 32
            -50 + 100 + -50                 | 0
            5 * 2 + 10                      | 20
            5 + 2 * 10                      | 25
            20 + 2 * -10                    | 0
            50 / 2 * 2 + 10                 | 60
            2 * (5 + 10)                    | 30
            3 * 3 * 3 + 10                  | 37
            3 * (3 * 3) + 10                | 37
            (5 + 10 * 2 + 15 / 3) * 2 + -10 | 50
            """)
    void testEvalIntegerExpression(String input, long expected) {
        MonkeyObject evaluated = eval(input);

        assertIntegerObject(evaluated, expected);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            true             | true
            false            | false
            1 < 2            | true
            1 > 2            | false
            1 < 1            | false
            1 > 1            | false
            1 == 1           | true
            1 != 1           | false
            1 == 2           | false
            1 != 2           | true
            true == true     | true
            false == false   | true
            true == false    | false
            true != false    | true
            false != true    | true
            (1 < 2) == true  | true
            (1 < 2) == false | false
            (1 > 2) == true  | false
            (1 > 2) == false | true
            # not in the book: == and != on different types compare, they don't raise a type mismatch
            5 == true        | false
            5 != true        | true
            """)
    void testEvalBooleanExpression(String input, boolean expected) {
        MonkeyObject evaluated = eval(input);

        assertBooleanObject(evaluated, expected);
    }

    @Test
    void testStringLiteral() {
        StringObject string = assertInstanceOf(StringObject.class, eval("\"Hello World!\""));

        assertEquals("Hello World!", string.value());
    }

    @Test
    void testStringConcatenation() {
        StringObject string = assertInstanceOf(
                StringObject.class,
                eval("\"Hello\" + \" \" + \"World!\"")
        );

        assertEquals("Hello World!", string.value());
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            len("")                 | 0
            len("four")             | 4
            len("hello world")      | 11
            len([])                 | 0
            len([1, 2, 3])          | 3
            len([1, 2 * 2, [3, 4]]) | 3
            # characters, not bytes or UTF-16 units
            len("한글")             | 2
            len("😀")               | 1
            """)
    void testLenBuiltin(String input, long expected) {
        assertIntegerObject(eval(input), expected);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', nullValues = "null", textBlock = """
            first([1, 2, 3])   | 1
            first([1 + 1, 3]) | 2
            first([])          | null
            """)
    void testFirstBuiltin(String input, Long expected) {
        MonkeyObject evaluated = eval(input);

        if (expected == null) {
            assertNullObject(evaluated);
        } else {
            assertIntegerObject(evaluated, expected);
        }
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', nullValues = "null", textBlock = """
            last([1, 2, 3])   | 3
            last([1, 2 + 2]) | 4
            last([])          | null
            """)
    void testLastBuiltin(String input, Long expected) {
        MonkeyObject evaluated = eval(input);

        if (expected == null) {
            assertNullObject(evaluated);
        } else {
            assertIntegerObject(evaluated, expected);
        }
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', nullValues = "null", textBlock = """
            rest([1, 2, 3]) | [2, 3]
            rest([1])       | []
            rest([])        | null
            """)
    void testRestBuiltin(String input, String expected) {
        MonkeyObject evaluated = eval(input);

        if (expected == null) {
            assertNullObject(evaluated);
        } else {
            ArrayObject array = assertInstanceOf(ArrayObject.class, evaluated);
            assertEquals(expected, array.inspect());
        }
    }

    @Test
    void testRestDoesNotModifyOriginalArray() {
        assertIntegerObject(eval("let a = [1, 2, 3]; rest(a); len(a)"), 3);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            push([], 1)          | [1]
            push([1, 2], 3)      | [1, 2, 3]
            push([1], 2 * 2)     | [1, 4]
            push([[1]], [2, 3])  | [[1], [2, 3]]
            """)
    void testPushBuiltin(String input, String expected) {
        ArrayObject array = assertInstanceOf(ArrayObject.class, eval(input));

        assertEquals(expected, array.inspect());
    }

    @Test
    void testPushDoesNotModifyOriginalArray() {
        assertIntegerObject(eval("let a = [1, 2]; push(a, 3); len(a)"), 2);
    }

    @Test
    void testPutsBuiltin() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        MonkeyObject evaluated;
        MonkeyObject evaluatedWithoutArguments;

        try (PrintStream capturedOut = new PrintStream(output, true, StandardCharsets.UTF_8)) {
            System.setOut(capturedOut);
            evaluated = eval("puts(\"hello\", 1, true)");
            evaluatedWithoutArguments = eval("puts()");
        } finally {
            System.setOut(originalOut);
        }

        assertNullObject(evaluated);
        assertNullObject(evaluatedWithoutArguments);
        assertEquals(
                "hello" + System.lineSeparator()
                        + "1" + System.lineSeparator()
                        + "true" + System.lineSeparator(),
                output.toString(StandardCharsets.UTF_8)
        );
    }

    @Test
    void testEmptyHashLiteral() {
        HashObject hash = assertInstanceOf(HashObject.class, eval("{}"));

        assertEquals(Map.of(), hash.pairs());
    }

    @Test
    void testHashInspectKeepsSourceOrder() {
        HashObject hash = assertInstanceOf(HashObject.class, eval("{\"one\": 1, \"two\": 2, \"three\": 3, \"four\": 4}"));

        assertEquals("{one: 1, two: 2, three: 3, four: 4}", hash.inspect());
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', nullValues = "null", textBlock = """
            {"foo": 5}["foo"]                                      | 5
            {"foo": 5}["bar"]                                      | null
            let key = "foo"; {"foo": 5}[key]                        | 5
            {}["foo"]                                                | null
            {5: 5}[5]                                                | 5
            {true: 5}[true]                                          | 5
            {false: 5}[false]                                        | 5
            {"one": 1 + 1}["one"]                                  | 2
            let two = "two"; {"one": 1, two: 2, "thr" + "ee": 3}["three"] | 3
            """)
    void testHashIndexExpressions(String input, Long expected) {
        MonkeyObject evaluated = eval(input);

        if (expected == null) {
            assertNullObject(evaluated);
        } else {
            assertIntegerObject(evaluated, expected);
        }
    }

    @Test
    void testArrayLiteral() {
        ArrayObject array = assertInstanceOf(ArrayObject.class, eval("[1, 2 * 2, 3 + 3]"));

        assertEquals(3, array.elements().size(), "elements size");
        assertIntegerObject(array.elements().get(0), 1);
        assertIntegerObject(array.elements().get(1), 4);
        assertIntegerObject(array.elements().get(2), 6);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', nullValues = "null", textBlock = """
            [1, 2, 3][0]              | 1
            [1, 2, 3][1]              | 2
            [1, 2, 3][2]              | 3
            let i = 0; [1][i]         | 1
            [1, 2, 3][1 + 1]          | 3
            let a = [1, 2, 3]; a[2]   | 3
            [1, 2, 3][3]              | null
            [1, 2, 3][-1]             | null
            """)
    void testArrayIndexExpressions(String input, Long expected) {
        MonkeyObject evaluated = eval(input);

        if (expected == null) {
            assertNullObject(evaluated);
        } else {
            assertIntegerObject(evaluated, expected);
        }
    }

    @Test
    void testBooleanObjectsAreReused() {
        // the book creates TRUE and FALSE once and hands out the same instances instead of allocating new ones
        assertSame(eval("true"), eval("true"));
        assertSame(eval("false"), eval("false"));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            !true   | false
            !false  | true
            !5      | false
            !!true  | true
            !!false | false
            !!5     | true
            # not in the book: NULL is falsy, so ! turns it into true
            !(if (false) { 1 }) | true
            """)
    void testBangOperator(String input, boolean expected) {
        MonkeyObject evaluated = eval(input);

        assertBooleanObject(evaluated, expected);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', nullValues = "null", textBlock = """
            if (true) { 10 }                          | 10
            if (false) { 10 }                         | null
            if (1) { 10 }                             | 10
            if (1 < 2) { 10 }                         | 10
            if (1 > 2) { 10 }                         | null
            if (1 > 2) { 10 } else { 20 }             | 20
            if (1 < 2) { 10 } else { 20 }             | 10
            # not in the book
            if (true) { 1; 2 }                        | 2
            if (false) { 1 } else { if (true) { 2 } } | 2
            if (if (false) { 1 }) { 10 } else { 20 }  | 20
            if (true) { if (true) {}; 5 }             | 5
            """)
    void testIfElseExpressions(String input, Long expected) {
        MonkeyObject evaluated = eval(input);

        if (expected == null) {
            assertNullObject(evaluated);
        } else {
            assertIntegerObject(evaluated, expected);
        }
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            return 10;                                            | 10
            return 10; 9;                                         | 10
            return 2 * 5; 9;                                      | 10
            9; return 2 * 5; 9;                                   | 10
            if (10 > 1) { if (10 > 1) { return 10; } return 1; }  | 10
            # not in the book: a return in the else branch also stops the outer program
            if (false) { 1 } else { return 5; 6 }; 7              | 5
            """)
    void testReturnStatements(String input, long expected) {
        MonkeyObject evaluated = eval(input);

        assertIntegerObject(evaluated, expected);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            5 + true;                                                       | type mismatch: INTEGER + BOOLEAN
            5 + true; 5;                                                    | type mismatch: INTEGER + BOOLEAN
            -true                                                           | unknown operator: -BOOLEAN
            true + false;                                                   | unknown operator: BOOLEAN + BOOLEAN
            5; true + false; 5                                              | unknown operator: BOOLEAN + BOOLEAN
            if (10 > 1) { true + false; }                                   | unknown operator: BOOLEAN + BOOLEAN
            if (10 > 1) { if (10 > 1) { return true + false; } return 1; }  | unknown operator: BOOLEAN + BOOLEAN
            # not in the book: an error inside an operand or a condition comes out unchanged
            (5 + true) + 1                                                  | type mismatch: INTEGER + BOOLEAN
            -(5 + true)                                                     | type mismatch: INTEGER + BOOLEAN
            !(5 + true)                                                     | type mismatch: INTEGER + BOOLEAN
            if (5 + true) { 1 }                                             | type mismatch: INTEGER + BOOLEAN
            return 5 + true; 1                                              | type mismatch: INTEGER + BOOLEAN
            foobar                                                          | identifier not found: foobar
            let a = 5 + true; a                                             | type mismatch: INTEGER + BOOLEAN
            let a = 5; a(1)                                                 | not a function: INTEGER
            let f = fn(x) { x }; f(5 + true)                                | type mismatch: INTEGER + BOOLEAN
            let f = fn(x) { let y = x; y }; f(1); y                         | identifier not found: y
            "Hello" - "World"                                               | unknown operator: STRING - STRING
            "Hello" + 5                                                     | type mismatch: STRING + INTEGER
            len(1)                                                          | argument to `len` not supported, got INTEGER
            len("one", "two")                                              | wrong number of arguments. got=2, want=1
            first(1)                                                        | argument to `first` must be ARRAY, got INTEGER
            first([1], [2])                                                 | wrong number of arguments. got=2, want=1
            last(1)                                                         | argument to `last` must be ARRAY, got INTEGER
            last([1], [2])                                                  | wrong number of arguments. got=2, want=1
            rest(1)                                                         | argument to `rest` must be ARRAY, got INTEGER
            rest([1], [2])                                                  | wrong number of arguments. got=2, want=1
            push(1, 2)                                                      | argument to `push` must be ARRAY, got INTEGER
            push([1])                                                       | wrong number of arguments. got=1, want=2
            {"name": "Monkey"}[fn(x) { x }]                                | unusable as hash key: FUNCTION
            {[1]: 1}                                                        | unusable as hash key: ARRAY
            [1][true]                                                       | index operator not supported: ARRAY
            fn(x, y) { x }(1)                                               | wrong number of arguments. got=1, want=2
            fn(x) { x }(1, 2)                                               | wrong number of arguments. got=2, want=1
            """)
    void testErrorHandling(String input, String expectedMessage) {
        MonkeyObject evaluated = eval(input);

        ErrorObject error = assertInstanceOf(ErrorObject.class, evaluated);
        assertEquals(expectedMessage, error.message());
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            let a = 5; a;                                | 5
            let a = 5 * 5; a;                            | 25
            let a = 5; let b = a; b;                     | 5
            let a = 5; let b = a; let c = a + b + 5; c;  | 15
            # not in WAIIG: rebinding a name that is already bound
            let a = 5; let a = a + 1; a                  | 6
            """)
    void testLetStatements(String input, long expected) {
        MonkeyObject evaluated = eval(input);

        assertIntegerObject(evaluated, expected);
    }

    @Test
    void testLetStatementProducesNoValue() {
        // a let only binds a name; like the REPL printing nothing after "let x = 5", eval returns null
        assertNull(eval("let a = 5;"));
    }

    @Test
    void testFunctionObject() {
        MonkeyObject evaluated = eval("fn(x) { x + 2; };");

        FunctionObject function = assertInstanceOf(FunctionObject.class, evaluated);
        assertEquals(1, function.params().size(), "params size");
        assertEquals("x", function.params().getFirst().toString());
        assertEquals("(x + 2)", function.body().toString());
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            let identity = fn(x) { x; }; identity(5);                  | 5
            let identity = fn(x) { return x; }; identity(5);           | 5
            let double = fn(x) { x * 2; }; double(5);                  | 10
            let add = fn(x, y) { x + y; }; add(5, 5);                  | 10
            let add = fn(x, y) { x + y; }; add(5 + 5, add(5, 5));      | 20
            fn(x) { x; }(5)                                            | 5
            # not in WAIIG
            # a return inside a function ends only that function, not the caller
            let f = fn() { return 1; 2 }; f(); 3                       | 3
            # the parameter x shadows the outer x without changing it
            let x = 10; let f = fn(x) { x }; f(1) + x                  | 11
            # recursion: fact finds itself through the environment it was defined in
            let fact = fn(n) { if (n == 0) { 1 } else { n * fact(n - 1) } }; fact(5) | 120
            # parameters are bound in order, so with a repeated name the last argument wins
            fn(x, x) { x }(1, 2)                                       | 2
            """)
    void testFunctionApplication(String input, long expected) {
        MonkeyObject evaluated = eval(input);

        assertIntegerObject(evaluated, expected);
    }

    @Test
    void testClosures() {
        String input = """
                let newAdder = fn(x) {
                  fn(y) { x + y };
                };

                let addTwo = newAdder(2);
                addTwo(2);
                """;

        assertIntegerObject(eval(input), 4);
    }

    // testEval in the book, plus a parser error check so a typo in the input fails loudly
    private static MonkeyObject eval(String input) {
        Parser parser = new Parser(new Lexer(input));
        Program program = parser.parseProgram();
        assertEquals(List.of(), parser.errors(), "parser errors");

        return Evaluator.eval(program, new Environment());
    }

    private static void assertIntegerObject(MonkeyObject object, long expected) {
        IntegerObject integer = assertInstanceOf(IntegerObject.class, object);
        assertEquals(expected, integer.value());
    }

    private static void assertBooleanObject(MonkeyObject object, boolean expected) {
        BooleanObject bool = assertInstanceOf(BooleanObject.class, object);
        assertEquals(expected, bool.value());
    }

    private static void assertNullObject(MonkeyObject object) {
        assertSame(NullObject.NULL, object);
    }
}
