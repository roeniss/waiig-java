package org.example.evaluator;

import org.example.ast.Program;
import org.example.lexer.Lexer;
import org.example.object.Environment;
import org.example.object.ErrorObject;
import org.example.object.MonkeyObject;
import org.example.object.ObjectType;
import org.example.object.QuoteObject;
import org.example.parser.Parser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class QuoteUnquoteTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            quote(5)               | 5
            quote(5 + 8)           | (5 + 8)
            # foobar and barfoo are never bound: the argument of quote is not evaluated
            quote(foobar)          | foobar
            quote(foobar + barfoo) | (foobar + barfoo)
            """)
    void testQuote(String input, String expected) {
        QuoteObject quote = assertQuoteObject(eval(input));

        assertNotNull(quote.node(), "quote.node()");
        assertEquals(expected, quote.node().toString());
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            quote(unquote(4))                                  | 4
            quote(unquote(4 + 4))                              | 8
            quote(8 + unquote(4 + 4))                          | (8 + 8)
            quote(unquote(4 + 4) + 8)                          | (8 + 8)
            let foobar = 8; quote(foobar)                      | foobar
            let foobar = 8; quote(unquote(foobar))             | 8
            quote(unquote(true))                               | true
            quote(unquote(true == false))                      | false
            quote(unquote(quote(4 + 4)))                       | (4 + 4)
            let quotedInfixExpression = quote(4 + 4); quote(unquote(4 + 4) + unquote(quotedInfixExpression)) | (8 + (4 + 4))
            # not in WAIIG: unquote deeper inside other kinds of nodes
            quote(add(unquote(1 + 1)))                         | add(2)
            quote([unquote(1 + 1), 3])                         | [2, 3]
            quote(fn(x) { x + unquote(1 + 1) })                | fn(x) (x + 2)
            # a string literal prints without its quotes
            quote(unquote("hi"))                               | hi
            """)
    void testQuoteUnquote(String input, String expected) {
        QuoteObject quote = assertQuoteObject(eval(input));

        assertNotNull(quote.node(), "quote.node()");
        assertEquals(expected, quote.node().toString());
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            # an error inside unquote comes out of quote unchanged, even from deep inside the tree
            quote(unquote(5 + true))         | type mismatch: INTEGER + BOOLEAN
            quote(8 + unquote(foobar))       | identifier not found: foobar
            # values that no source code can stand for
            quote(unquote(fn(x) { x }))      | cannot unquote FUNCTION
            quote(unquote(len))              | cannot unquote BUILTIN
            quote(unquote([1, 2]))           | cannot unquote ARRAY
            """)
    void testQuoteUnquoteErrors(String input, String expectedMessage) {
        MonkeyObject evaluated = eval(input);

        ErrorObject error = assertInstanceOf(ErrorObject.class, evaluated,
                () -> "got " + (evaluated == null ? "null" : evaluated.inspect()));
        assertEquals(expectedMessage, error.message());
    }

    @Test
    void testQuoteObjectTypeAndInspect() {
        QuoteObject quote = assertQuoteObject(eval("quote(5 + 8)"));

        assertEquals(ObjectType.QUOTE, quote.type());
        assertEquals("QUOTE((5 + 8))", quote.inspect());
    }

    // the message shows the value itself, so an ErrorObject reveals its error text instead of just its class
    private static QuoteObject assertQuoteObject(MonkeyObject evaluated) {
        return assertInstanceOf(QuoteObject.class, evaluated,
                () -> "got " + (evaluated == null ? "null" : evaluated.inspect()));
    }

    private static MonkeyObject eval(String input) {
        Parser parser = new Parser(new Lexer(input));
        Program program = parser.parseProgram();
        assertEquals(List.of(), parser.errors(), "parser errors");

        return Evaluator.eval(program, new Environment());
    }
}
