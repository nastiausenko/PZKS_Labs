package dev.nastiausenko;

import java.util.List;

public class ExpressionParser {
    private final List<Token> tokens;
    private int position;

    public ExpressionParser(List<Token> tokens) {
        this.tokens = tokens;
        this.position = 0;
    }

    public ExpressionNode parse() {
        ExpressionNode root = parseExpression();

        if (currentToken().getType() != TokenType.END) {
            throw new IllegalArgumentException(
                    "Неочікуваний токен '" + currentToken().getValue() + "' на позиції " + currentToken().getPosition()
            );
        }

        return root;
    }

    private ExpressionNode parseExpression() {
        ExpressionNode left = parseTerm();

        while (match(TokenType.PLUS) || match(TokenType.MINUS)) {
            Token operator = previousToken();
            ExpressionNode right = parseTerm();

            left = new ExpressionNode(
                    operator.getType(),
                    operator.getValue(),
                    operator.getPosition(),
                    left,
                    right
            );
        }

        return left;
    }

    private ExpressionNode parseTerm() {
        ExpressionNode left = parseUnary();

        while (match(TokenType.MULTIPLY) || match(TokenType.DIVIDE)) {
            Token operator = previousToken();
            ExpressionNode right = parseUnary();

            left = new ExpressionNode(
                    operator.getType(),
                    operator.getValue(),
                    operator.getPosition(),
                    left,
                    right
            );
        }

        return left;
    }

    private ExpressionNode parseUnary() {
        if (match(TokenType.MINUS)) {
            Token operator = previousToken();
            ExpressionNode operand = parseUnary();

            return new ExpressionNode(TokenType.MINUS, "unary-", operator.getPosition(), operand);
        }

        return parsePrimary();
    }

    private ExpressionNode parsePrimary() {
        if (match(TokenType.NUMBER) || match(TokenType.IDENTIFIER) || match(TokenType.CONSTANT)) {
            Token token = previousToken();

            return new ExpressionNode(
                    token.getType(),
                    token.getValue(),
                    token.getPosition()
            );
        }

        if (match(TokenType.LEFT_PAREN)) {
            ExpressionNode node = parseExpression();
            consume(TokenType.RIGHT_PAREN, "Очікувалася закриваюча дужка ')'.");
            return node;
        }

        if (match(TokenType.FUNCTION)) {
            Token function = previousToken();

            consume(TokenType.LEFT_PAREN, "Після функції '" + function.getValue() + "' очікувалася відкриваюча дужка '('.");

            ExpressionNode node = new ExpressionNode(
                    TokenType.FUNCTION,
                    function.getValue(),
                    function.getPosition()
            );

            do {
                node.addChild(parseExpression());
            } while (match(TokenType.COMMA));

            consume(TokenType.RIGHT_PAREN, "Очікувалася закриваюча дужка ')'.");

            return node;
        }

        throw new IllegalArgumentException("Неочікуваний токен '" + currentToken().getValue() + "' на позиції " + currentToken().getPosition());
    }

    private boolean match(TokenType type) {
        if (currentToken().getType() != type) {
            return false;
        }

        position++;
        return true;
    }

    private void consume(TokenType type, String message) {
        if (!match(type)) {
            throw new IllegalArgumentException(message + " Позиція: " + currentToken().getPosition());
        }
    }

    private Token currentToken() {
        return tokens.get(position);
    }

    private Token previousToken() {
        return tokens.get(position - 1);
    }
}