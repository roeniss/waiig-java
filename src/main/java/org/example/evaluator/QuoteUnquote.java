package org.example.evaluator;

import org.example.ast.*;
import org.example.object.*;
import org.example.token.Token;
import org.example.token.TokenType;

public class QuoteUnquote {

    static MonkeyObject quote(Expression node, Environment env) {
        try {
            Expression newNode = evalUnquoteCalls(node, env);
            return new QuoteObject(newNode);
        } catch (UnquoteFailure failure) {
            return failure.error;
        }
    }

    // The modifier can only hand back an Expression, so an error found while unquoting
    // leaves the tree walk by exception and is turned back into an ErrorObject in quote().
    private static final class UnquoteFailure extends RuntimeException {
        private final ErrorObject error;

        private UnquoteFailure(ErrorObject error) {
            super(error.message(), null, false, false); // control flow only: no stack trace needed
            this.error = error;
        }
    }

    private static Expression evalUnquoteCalls(Expression quoted, Environment env) {
        return Modifier.modify(quoted, expression -> {
            if (expression instanceof CallExpression call && call.function().tokenLiteral().equals("unquote")) {
                MonkeyObject unquoted = Evaluator.eval(call.arguments().getFirst(), env);
                return convertObjectToASTNode(unquoted);
            }else {
                return expression;
            }
        });
    }

    private static Expression convertObjectToASTNode(MonkeyObject unquoted) {
        return switch(unquoted) {
            case BooleanObject(boolean value) -> new BooleanLiteral(new Token(value? TokenType.TRUE : TokenType.FALSE , String.valueOf(value)), value);
            case IntegerObject(long value) -> new NumberLiteral(new Token(TokenType.NUMBER, String.valueOf(value)), value);
            case QuoteObject(Expression node) -> node;
            case StringObject(String value) ->  new StringLiteral(new Token(TokenType.STRING, value),value);
            case ErrorObject errorObject -> throw new UnquoteFailure(errorObject);
            // no source code can stand for these values: a function would lose its closure,
            // a builtin has no name to write back, and Monkey has no null literal
            case ArrayObject _, BuiltinObject _, FunctionObject _, HashObject _, MacroObject _, NullObject _, ReturnObject _ ->
                    throw new UnquoteFailure(new ErrorObject("cannot unquote " + unquoted.type()));
        };
    }


}
