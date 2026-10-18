package org.example.parser;

import org.example.ast.*;
import org.example.lexer.Lexer;
import org.example.token.Token;
import org.example.token.TokenType;

import java.util.*;

public class Parser {
    private final Lexer lexer;
    private final List<String> errors = new ArrayList<>();

    private Token curToken;
    private Token peekToken;

    private final Map<TokenType, PrefixParseFn> prefixParseFns = new HashMap<>();
    private final Map<TokenType, InfixParseFn> infixParseFns = new HashMap<>();

    public Parser(Lexer lexer) {
        this.lexer = lexer;
        nextToken();
        nextToken();

        registerPrefix(TokenType.IDENTIFIER, this::parseIdentifier);
        registerPrefix(TokenType.NUMBER, this::parseNumberLiteral);
        registerPrefix(TokenType.MINUS, this::parsePrefixExpression);
        registerPrefix(TokenType.BANG, this::parsePrefixExpression);
        registerPrefix(TokenType.TRUE, this::parseBooleanExpression);
        registerPrefix(TokenType.FALSE, this::parseBooleanExpression);
        registerPrefix(TokenType.LPAREN, this::parseGroupedExpression);
        registerPrefix(TokenType.IF, this::parseIfExpression);
        registerPrefix(TokenType.FUNCTION, this::parseFunctionExpression);
        registerPrefix(TokenType.STRING, this::parseStringExpression);
        registerPrefix(TokenType.LBRACKET, this::parseArrayLiteral);
        registerPrefix(TokenType.MACRO, this::parseMacroLiteral);
        registerPrefix(TokenType.LBRACE, this::parseHashLiteral);
        registerPrefix(TokenType.MACRO, this::parseMacroLiteral);

        registerInfix(TokenType.PLUS, this::parseInfixExpression);
        registerInfix(TokenType.MINUS, this::parseInfixExpression);
        registerInfix(TokenType.SLASH, this::parseInfixExpression);
        registerInfix(TokenType.ASTERISK, this::parseInfixExpression);
        registerInfix(TokenType.EQ, this::parseInfixExpression);
        registerInfix(TokenType.NEQ, this::parseInfixExpression);
        registerInfix(TokenType.LT, this::parseInfixExpression);
        registerInfix(TokenType.GT, this::parseInfixExpression);
        registerInfix(TokenType.LPAREN, this::parseCallExpression);
        registerInfix(TokenType.LBRACKET, this::parseIndexExpression);
    }

    private Expression parseMacroLiteral() {
        Token token = curToken;

        if (!expectPeek(TokenType.LPAREN)){
            return null;
        }

        List<Identifier> identifiers = parseFunctionParameters();

        if (!expectPeek(TokenType.LBRACE)){
            return null;
        }

        BlockStatement body = parseBlockStatement();

        return new MacroLiteral(curToken, identifiers, body);
    }

    private Expression parseHashLiteral() {
        Token token = curToken;
        Map<Expression, Expression> pairs = new LinkedHashMap<>();

        while (peekToken.type() != TokenType.RBRACE) {
            nextToken();
            Expression key = parseExpression(Precedence.LOWEST);

            if (!expectPeek(TokenType.COLON)) {
                return null;
            }

            nextToken();
            Expression value = parseExpression(Precedence.LOWEST);
            pairs.put(key, value);

            if (peekToken.type() != TokenType.RBRACE && !expectPeek(TokenType.COMMA)) {
                return null;
            }
        }

        if (!expectPeek(TokenType.RBRACE)) {
            return null;
        }

        return new HashLiteral(token, pairs);
    }

    private Expression parseIndexExpression(Expression left) {
        Token token = curToken;

        nextToken();
        Expression index = parseExpression(Precedence.LOWEST);

        if (!expectPeek(TokenType.RBRACKET)) {
            return null;
        }

        return new IndexExpression(token, left, index);
    }

    private Expression parseArrayLiteral() {
        Token token = curToken;

        List<Expression> elements = parseExpressionList(TokenType.RBRACKET);

        return new ArrayLiteral(token, elements);
    }

    private List<Expression> parseExpressionList(TokenType end) {
        List<Expression> list = new ArrayList<>();

        if (peekToken.type() == end) {
            nextToken();
            return list;
        }

        nextToken();
        list.add(parseExpression(Precedence.LOWEST));

        while (peekToken.type() == TokenType.COMMA) {
            nextToken();
            nextToken();
            list.add(parseExpression(Precedence.LOWEST));
        }

        if (!expectPeek(end)) {
            return null;
        }
        return list;
    }

    private Expression parseStringExpression() {
        return new StringLiteral(curToken, curToken.literal());
    }

    private Expression parseCallExpression(Expression function) {
        return new CallExpression(curToken, function, parseExpressionList(TokenType.RPAREN));
    }

    private List<Identifier> parseFunctionParameters() {
        List<Identifier> identifiers = new ArrayList<>();

        if (peekToken.type() == TokenType.RPAREN) {
            nextToken();
            return identifiers;
        }

        nextToken();

        identifiers.add(new Identifier(curToken, curToken.literal()));

        while (peekToken.type() == TokenType.COMMA) {
            nextToken(); // ","
            nextToken(); // identifier
            identifiers.add(new Identifier(curToken, curToken.literal()));
        }

        if (!expectPeek(TokenType.RPAREN)) {
            return null;
        }

        return identifiers;
    }

    private Expression parseFunctionExpression() {
        Token token = curToken;

        if (!expectPeek(TokenType.LPAREN)) {
            return null;
        }

        List<Identifier> parameters = parseFunctionParameters();

        if (!expectPeek(TokenType.LBRACE)) {
            return null;
        }

        BlockStatement body = parseBlockStatement();

        return new FunctionLiteral(token, parameters, body);
    }

    private BlockStatement parseBlockStatement() {
        Token token = curToken;
        List<Statement> statements = new ArrayList<>();

        nextToken();

        while (curToken.type() != TokenType.RBRACE && curToken.type() != TokenType.EOF) {
            Statement statement = parseStatement();
            if (statement != null) {
                statements.add(statement);
            }
            nextToken();
        }

        return new BlockStatement(token, statements);
    }

    private Expression parseIfExpression() {
        Token token = curToken;

        if (!expectPeek(TokenType.LPAREN)) {
            return null;
        }
        nextToken();

        Expression condition = parseExpression(Precedence.LOWEST);

        if (!expectPeek(TokenType.RPAREN)) {
            return null;
        }

        if (!expectPeek(TokenType.LBRACE)) {
            return null;
        }

        BlockStatement consequence = parseBlockStatement();

        BlockStatement alternative = null;
        if (peekToken.type() == TokenType.ELSE) {
            nextToken();

            if (!expectPeek(TokenType.LBRACE)) {
                return null;
            }

            alternative = parseBlockStatement();
        }

        return new IfExpression(token, condition, consequence, alternative);
    }

    private Expression parseGroupedExpression() {
        nextToken();

        Expression expression = parseExpression(Precedence.LOWEST);

        if (!expectPeek(TokenType.RPAREN)) {
            return null;
        }

        return expression;
    }

    private Expression parseBooleanExpression() {
        return new BooleanLiteral(curToken, curToken.type() == TokenType.TRUE);
    }

    private Expression parseInfixExpression(Expression left) {
        Token token = curToken;
        String operator = curToken.literal();

        Precedence precedence = curPrecedence();
        nextToken();
        Expression right = parseExpression(precedence);

        return new InfixExpression(token, left, operator, right);

    }

    private Expression parsePrefixExpression() {
        Token token = curToken;
        String operator = curToken.literal();

        nextToken();

        Expression rightExpression = parseExpression(Precedence.PREFIX);

        return new PrefixExpression(token, operator, rightExpression);
    }

    private Expression parseNumberLiteral() {
        Token token = curToken;
        try {
            long value = Long.parseLong(token.literal());
            return new NumberLiteral(token, value);
        } catch (NumberFormatException e) {
            errors.add("Invalid number literal: " + token.literal());
            return null;
        }
    }

    private Expression parseIdentifier() {
        return new Identifier(curToken, curToken.literal());
    }

    public List<String> errors() {
        return List.copyOf(errors);
    }

    private void nextToken() {
        curToken = peekToken;
        peekToken = lexer.nextToken();
    }

    public Program parseProgram() {
        ArrayList<Statement> statements = new ArrayList<>();

        while (curToken.type() != TokenType.EOF) {
            Statement s = parseStatement();
            if (s != null) {
                statements.add(s);
            }
            nextToken();
        }

        return new Program(statements);
    }

    private Precedence peekPrecedence() {
        return Precedence.getPrecedence(peekToken.type());
    }

    private Precedence curPrecedence() {
        return Precedence.getPrecedence(curToken.type());
    }

    private Statement parseStatement() {
        return switch (curToken.type()) {
            case TokenType.LET -> parseLetStatement();
            case TokenType.RETURN -> parseReturnStatement();
            default -> parseExpressionStatement();
        };
    }

    private Expression parseExpression(Precedence precedence) {
        PrefixParseFn prefixParseFn = prefixParseFns.get(curToken.type());

        if (prefixParseFn == null) {
            noPrefixParseFnError(curToken.type());
            return null;
        }

        Expression leftExp = prefixParseFn.parse();

        while (peekToken.type() != TokenType.SEMICOLON && precedence.isLowerThan(peekPrecedence())) {
            InfixParseFn infixParseFn = infixParseFns.get(peekToken.type());
            if (infixParseFn == null) {
                return leftExp;
            }

            nextToken();

            leftExp = infixParseFn.parse(leftExp);
        }

        return leftExp;
    }

    private Statement parseExpressionStatement() {
        Token token = curToken;
        Expression expression = parseExpression(Precedence.LOWEST);

        if (peekToken.type() == TokenType.SEMICOLON) {
            nextToken();
        }

        return new ExpressionStatement(token, expression);
    }

    private Statement parseReturnStatement() {
        Token returnToken = curToken;

        nextToken();

        Expression returnValue = parseExpression(Precedence.LOWEST);

        if (peekToken.type() == TokenType.SEMICOLON) {
            nextToken();
        }

        return new ReturnStatement(returnToken, returnValue);
    }

    private Statement parseLetStatement() {
        Token letToken = curToken;
        if (!expectPeek(TokenType.IDENTIFIER)) {
            return null;
        }

        Identifier name = new Identifier(curToken, curToken.literal());

        if (!expectPeek(TokenType.ASSIGN)) {
            return null;
        }

        nextToken();

        Expression value = parseExpression(Precedence.LOWEST);

        if (peekToken.type() == TokenType.SEMICOLON) {
            nextToken();
        }

        return new LetStatement(letToken, name, value);
    }

    private boolean expectPeek(TokenType expected) {
        if (peekToken.type() == expected) {
            nextToken();
            return true;
        } else {
            peekError(expected);
            return false;
        }
    }

    private void noPrefixParseFnError(TokenType type) {
        errors.add("no prefix parse function for %s found".formatted(type));
    }

    private void peekError(TokenType expected) {
        errors.add("expected next token to be %s, got %s instead".formatted(expected, peekToken.type()));
    }

    private void registerPrefix(TokenType type, PrefixParseFn fn) {
        prefixParseFns.put(type, fn);
    }

    private void registerInfix(TokenType type, InfixParseFn fn) {
        infixParseFns.put(type, fn);
    }
}
