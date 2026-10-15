package org.example.evaluator;

import org.example.ast.*;
import org.example.object.*;

import java.util.*;

public class Evaluator {

    public static MonkeyObject eval(Node node, Environment env) {
        return switch (node) {
            case Program program -> evalProgram(program.statements(), env);
            case ExpressionStatement expressionStatement -> eval(expressionStatement.expression(), env);
            case NumberLiteral numberLiteral -> new IntegerObject(numberLiteral.value());
            case BooleanLiteral booleanLiteral -> evalBooleanObject(booleanLiteral.value());
            case PrefixExpression prefixExpression -> evalPrefixExpression(prefixExpression.operator(), eval(prefixExpression.right(), env));
            case InfixExpression infixExpression -> evalInfixExpression(infixExpression, env);
            case BlockStatement blockStatement -> evalBlockStatements(blockStatement.statements(), env);
            case IfExpression ifExpression -> evalIfExpression(ifExpression, env);
            case ReturnStatement returnStatement -> evalReturnStatement(returnStatement, env);
            case LetStatement letStatement -> evalLetStatement(letStatement, env);
            case Identifier identifier -> evalIdentifier(identifier, env);
            case FunctionLiteral functionLiteral -> new FunctionObject(functionLiteral.parameters(), functionLiteral.body(), env);
            case StringLiteral stringLiteral -> new StringObject(stringLiteral.value());
            case CallExpression callExpression -> evalCallExpression(callExpression, env);
            case ArrayLiteral arrayLiteral -> evalArrayLiteral(arrayLiteral, env);
            case IndexExpression indexExpression -> evalIndexExpression(indexExpression, env);
            case HashLiteral hashLiteral -> evalHashLiteral(hashLiteral, env);
        };
    }

    private static MonkeyObject evalHashLiteral(HashLiteral hashLiteral, Environment env) {
        Map<Expression, Expression> hashLiteralPairs = hashLiteral.pairs();

        Map<HashKey, HashPair> hashPairs = new LinkedHashMap<>();

        for (Map.Entry<Expression, Expression> entry : hashLiteralPairs.entrySet()) {
            Expression keyNode = entry.getKey();
            MonkeyObject key = eval(keyNode, env);
            if (key instanceof ErrorObject) {
                return key;
            }
            if (!(key instanceof Hashable hashable)) {
                return newError("unusable as hash key: %s", key.type());
            }

            Expression valueNode = entry.getValue();
            MonkeyObject value = eval(valueNode, env);
            if (value instanceof ErrorObject) {
                return value;
            }

            HashKey hashed = hashable.hashKey();
            HashPair hashPair = new HashPair(key, value);

            hashPairs.put(hashed, hashPair);
        }

        return new HashObject(hashPairs);
    }

    private static MonkeyObject evalIndexExpression(IndexExpression indexExpression, Environment env) {
        MonkeyObject left = eval(indexExpression.left(), env);
        if (left instanceof ErrorObject) {
            return left;
        }

        MonkeyObject index = eval(indexExpression.index(), env);
        if (index instanceof ErrorObject) {
            return index;
        }

        return switch (left) {
            case ArrayObject(List<MonkeyObject> elements)
                    when index instanceof IntegerObject(long value) ->
                    evalArrayIndexExpression(elements, value);
            case HashObject(Map<HashKey, HashPair> pairs) ->
                    evalHashIndexExpression(pairs, index);
            default -> new ErrorObject("index operator not supported: " + left.type());
        };
    }

    private static MonkeyObject evalHashIndexExpression(Map<HashKey, HashPair> pairs, MonkeyObject index) {
        if (!(index instanceof Hashable hashable)) {
            return newError("unusable as hash key: %s", index.type());
        }

        HashPair hashPair = pairs.get(hashable.hashKey());
        if (hashPair == null) {
            return NullObject.NULL;
        }
        return hashPair.value();
    }

    private static MonkeyObject evalArrayIndexExpression(List<MonkeyObject> elements, long index) {
        if (index < 0 || index >= elements.size()) {
            return NullObject.NULL;
        }
        return elements.get((int) index);
    }

    private static MonkeyObject evalArrayLiteral(ArrayLiteral node, Environment env) {
        List<MonkeyObject> elements = evalExpressions(node.elements(), env);
        if (elements.size() == 1 && elements.getFirst() instanceof ErrorObject) {
            return elements.getFirst();
        }
        return new ArrayObject(elements);
    }

    private static MonkeyObject applyFunction(MonkeyObject function, List<MonkeyObject> arguments) {
        return switch (function) {
            case FunctionObject(List<Identifier> params, BlockStatement body, Environment env) -> {
                if (arguments.size() != params.size()) {
                    yield newError("wrong number of arguments. got=%d, want=%d", arguments.size(), params.size());
                }
                Environment extendedEnv = extendFunctionEnv(env, params, arguments);
                MonkeyObject evaluated = eval(body, extendedEnv);
                yield unwrapReturnValue(evaluated);
            }
            case BuiltinObject(BuiltinFunction builtin) -> builtin.apply(arguments);
            default -> new ErrorObject("not a function: " + function.type());
        };
    }

    private static MonkeyObject unwrapReturnValue(MonkeyObject evaluated) {
        if (evaluated instanceof ReturnObject(MonkeyObject value)) {
            return value;
        }
        return evaluated;
    }

    private static Environment extendFunctionEnv(Environment env, List<Identifier> params, List<MonkeyObject> arguments) {
        Environment newEnv = new Environment(env);

        for (int i = 0; i < params.size(); i++) {
            newEnv.set(params.get(i).value(), arguments.get(i));
        }

        return newEnv;
    }

    private static MonkeyObject evalCallExpression(CallExpression callExpression, Environment env) {
        MonkeyObject function = eval(callExpression.function(), env);
        if (function instanceof ErrorObject) {
            return function;
        }

        List<MonkeyObject> arguments = evalExpressions(callExpression.arguments(), env);
        if (arguments.size() == 1 && arguments.getFirst() instanceof ErrorObject) {
            return arguments.getFirst();
        }

        return applyFunction(function, arguments);
    }

    private static List<MonkeyObject> evalExpressions(List<Expression> arguments, Environment env) {
        List<MonkeyObject> result = new ArrayList<>();

        for (Expression arg : arguments) {
            MonkeyObject evaluated = eval(arg, env);
            if (evaluated instanceof ErrorObject) {
                return List.of(evaluated);
            }

            result.add(evaluated);
        }

        return result;
    }

    private static MonkeyObject evalIdentifier(Identifier identifier, Environment env) {
        Optional<MonkeyObject> object = env.get(identifier.value());
        if (object.isPresent()) {
            return object.get();
        }

        Optional<BuiltinObject> builtin = Builtins.get(identifier.value());
        if (builtin.isPresent()) {
            return builtin.get();
        }

        return new ErrorObject("identifier not found: " + identifier.value());
    }

    private static MonkeyObject evalLetStatement(LetStatement letStatement, Environment env) {
        MonkeyObject value = eval(letStatement.value(), env);
        if (value instanceof ErrorObject) {
            return value;
        }
        env.set(letStatement.name().value(), value);
        return null;
    }

    private static MonkeyObject evalReturnStatement(ReturnStatement returnStatement, Environment env) {
        MonkeyObject value = eval(returnStatement.value(), env);
        if (value instanceof ErrorObject) {
            return value;
        }
        return new ReturnObject(value);
    }

    private static MonkeyObject evalIfExpression(IfExpression ifExpression, Environment env) {
        MonkeyObject condition = eval(ifExpression.condition(), env);
        if (condition instanceof ErrorObject) {
            return condition;
        }

        if (isTruthy(condition)) {
            return eval(ifExpression.consequence(), env);
        } else if (ifExpression.alternative() != null) {
            return eval(ifExpression.alternative(), env);
        } else {
            return NullObject.NULL;
        }
    }

    private static boolean isTruthy(MonkeyObject object) {
        if (object == NullObject.NULL) {
            return false;
        } else if (object == BooleanObject.FALSE) {
            return false;
        } else {
            return true;
        }
    }

    private static MonkeyObject evalIntegerInfixExpression(String operator, long leftValue, long rightValue) {
        return switch (operator) {
            case "+" -> new IntegerObject(leftValue + rightValue);
            case "-" -> new IntegerObject(leftValue - rightValue);
            case "*" -> new IntegerObject(leftValue * rightValue);
            case "/" -> new IntegerObject(leftValue / rightValue);
            case "<" -> evalBooleanObject(leftValue < rightValue);
            case ">" -> evalBooleanObject(leftValue > rightValue);
            case "==" -> evalBooleanObject(leftValue == rightValue);
            case "!=" -> evalBooleanObject(leftValue != rightValue);
            default -> newError("unknown operator: %s %s %s", ObjectType.INTEGER, operator, ObjectType.INTEGER);
        };
    }

    private static MonkeyObject evalInfixExpression(InfixExpression infixExpression, Environment env) {
        MonkeyObject left = eval(infixExpression.left(), env);
        if (left instanceof ErrorObject) {
            return left;
        }
        MonkeyObject right = eval(infixExpression.right(), env);
        if (right instanceof ErrorObject) {
            return right;
        }
        if (left instanceof StringObject(String leftValue) && right instanceof StringObject(String rightValue)) {
            return evalStringInfixExpression(infixExpression.operator(), leftValue, rightValue);
        }
        return evalInfixExpression(infixExpression.operator(), left, right);
    }

    private static MonkeyObject evalStringInfixExpression(String operator, String leftValue, String rightValue) {
        return switch (operator) {
            case "+" -> new StringObject(leftValue + rightValue);
            default -> newError("unknown operator: %s %s %s", ObjectType.STRING, operator, ObjectType.STRING);
        };
    }

    private static MonkeyObject evalInfixExpression(String operator, MonkeyObject left, MonkeyObject right) {
        if (left instanceof IntegerObject(long leftValue) && right instanceof IntegerObject(long rightValue)) {
            return evalIntegerInfixExpression(operator, leftValue, rightValue);
        }
        return switch (operator) {
            case "==" -> evalBooleanObject(left == right);
            case "!=" -> evalBooleanObject(left != right);
            default -> left.type() != right.type()
                ? newError("type mismatch: %s %s %s", left.type(), operator, right.type())
                : newError("unknown operator: %s %s %s", left.type(), operator, right.type());
        };
    }

    private static MonkeyObject evalPrefixExpression(String operator, MonkeyObject right) {
        if (right instanceof ErrorObject) {
            return right;
        }
        return switch (operator) {
            case "!" -> evalBangOperatorExpression(right);
            case "-" -> evalMinusOperatorExpression(right);
            default -> newError("unknown operator: %s%s", operator, right.type());
        };
    }

    private static MonkeyObject evalMinusOperatorExpression(MonkeyObject right) {
        return switch (right) {
            case IntegerObject(long value) -> new IntegerObject(-value);
            default -> newError("unknown operator: -%s", right.type());
        };
    }

    private static MonkeyObject evalBangOperatorExpression(MonkeyObject right) {
        if (isTruthy(right)) {
            return BooleanObject.FALSE;
        } else {
            return BooleanObject.TRUE;
        }
    }

    private static BooleanObject evalBooleanObject(boolean value) {
        return value ? BooleanObject.TRUE : BooleanObject.FALSE;
    }

    private static MonkeyObject evalBlockStatements(List<Statement> statements, Environment env) {
        MonkeyObject monkeyObject = null;
        for (Statement statement : statements) {
            monkeyObject = eval(statement, env);

            if (monkeyObject instanceof ReturnObject || monkeyObject instanceof ErrorObject) {
                return monkeyObject;
            }
        }
        return monkeyObject;
    }

    private static MonkeyObject evalProgram(List<Statement> statements, Environment env) {
        MonkeyObject monkeyObject = null;
        for (Statement statement : statements) {
            monkeyObject = eval(statement, env);

            if (monkeyObject instanceof ReturnObject(MonkeyObject returnValue)) {
                return returnValue;
            }
            if (monkeyObject instanceof ErrorObject) {
                return monkeyObject;
            }
        }
        return monkeyObject;
    }

    private static ErrorObject newError(String format, Object... args) {
        return new ErrorObject(format.formatted(args));
    }
}
