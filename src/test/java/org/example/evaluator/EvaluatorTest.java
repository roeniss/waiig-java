package org.example.evaluator;

import org.example.ast.Program;
import org.example.lexer.Lexer;
import org.example.object.BooleanObject;
import org.example.object.Environment;
import org.example.object.ErrorObject;
import org.example.object.FunctionObject;
import org.example.object.IntegerObject;
import org.example.object.MonkeyObject;
import org.example.object.NullObject;
import org.example.parser.Parser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

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
