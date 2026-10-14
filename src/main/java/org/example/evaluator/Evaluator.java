package org.example.evaluator;

import org.example.ast.*;
import org.example.object.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
            case CallExpression callExpression -> evalCallExpression(callExpression, env);
        };
    }

    private static MonkeyObject applyFunction(MonkeyObject function, List<MonkeyObject> arguments) {
        if (!(function instanceof  FunctionObject(List<Identifier> params, BlockStatement body, Environment env))){
            return new ErrorObject("not a function: " + function.type());
        }

        Environment extendedEnv = extendFunctionEnv(env, params, arguments);
        MonkeyObject evaluated = eval(body, extendedEnv);
        return unwrapReturnValue(evaluated);
    }

    private static MonkeyObject unwrapReturnValue(MonkeyObject evaluated) {
        if (evaluated instanceof ReturnObject(MonkeyObject value)) {
            return value;
        }
        return evaluated;
    }

    private static Environment extendFunctionEnv(Environment env, List<Identifier> params, List<MonkeyObject> arguments) {
        Environment newEnv = new Environment(env);

        for (Identifier param : params) {
            newEnv.set(param.value(), arguments.get(params.indexOf(param)));
        }

        return newEnv;
    }

    private static MonkeyObject evalCallExpression(CallExpression callExpression, Environment env) {
        MonkeyObject function = eval(callExpression.function(), env);
        if (function instanceof ErrorObject) {
            return function;
        }

        List<MonkeyObject> arguments = evalExpressions(callExpression.arguments(), env);
        if( arguments.size() == 1 && arguments.getFirst() instanceof ErrorObject) {
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
        return object.orElseGet(() -> new ErrorObject("identifier not found: " + identifier.value()));
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
        return evalInfixExpression(infixExpression.operator(), left, right);
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
        if (!(right instanceof IntegerObject(long value))) {
            return newError("unknown operator: -%s", right.type());
        }
        return new IntegerObject(-value);
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
