package dev.nastiausenko;

import java.util.ArrayList;
import java.util.List;

public class ExpressionOptimizer {

    public ExpressionNode optimize(ExpressionNode node) {
        if (node == null || node.isLeaf()) {
            return node;
        }

        List<ExpressionNode> optimizedChildren = new ArrayList<>();

        for (ExpressionNode child : node.getChildren()) {
            optimizedChildren.add(optimize(child));
        }

        node.getChildren().clear();

        for (ExpressionNode child : optimizedChildren) {
            node.addChild(child);
        }

        if (node.getType() == TokenType.DIVIDE) {
            ExpressionNode denominator = node.getChildren().get(1);

            if (isZero(denominator)) {
                throw new ArithmeticException(
                        "Ділення на 0 на позиції " + node.getPosition()
                );
            }
        }

        if (isArithmeticOperator(node) && hasTwoNumericChildren(node)) {
            return calculate(node);
        }

        if ("unary-".equals(node.getValue()) && node.getChildren().size() == 1 && isNumber(node.getChildren().get(0))) {
            double value = -getNumber(node.getChildren().get(0));
            return createNumber(value, node.getPosition());
        }

        if (node.getChildren().size() == 2) {
            ExpressionNode left = node.getChildren().get(0);
            ExpressionNode right = node.getChildren().get(1);

            switch (node.getType()) {
                case PLUS:
                    if (isZero(left)) {
                        return right;
                    }

                    if (isZero(right)) {
                        return left;
                    }

                    break;

                case MULTIPLY:
                    if (isZero(left) || isZero(right)) {
                        return createNumber(0, node.getPosition());
                    }

                    if (isOne(left)) {
                        return right;
                    }

                    if (isOne(right)) {
                        return left;
                    }

                    break;

                case DIVIDE:
                    if (isOne(right)) {
                        return left;
                    }

                    break;

                default:
                    break;
            }
        }

        if (node.getType() == TokenType.PLUS) {
            return combineAdditionConstants(node);
        }

        return node;
    }

    private boolean isArithmeticOperator(ExpressionNode node) {
        return node.getType() == TokenType.PLUS
                || node.getType() == TokenType.MINUS
                || node.getType() == TokenType.MULTIPLY
                || node.getType() == TokenType.DIVIDE;
    }

    private boolean hasTwoNumericChildren(ExpressionNode node) {
        return node.getChildren().size() == 2
                && isNumber(node.getChildren().get(0))
                && isNumber(node.getChildren().get(1));
    }

    private ExpressionNode calculate(ExpressionNode node) {
        double left = getNumber(node.getChildren().get(0));
        double right = getNumber(node.getChildren().get(1));

        double result;

        switch (node.getType()) {
            case PLUS:
                result = left + right;
                break;

            case MINUS:
                result = left - right;
                break;

            case MULTIPLY:
                result = left * right;
                break;

            case DIVIDE:
                if (right == 0.0) {
                    throw new ArithmeticException(
                            "Ділення на 0 на позиції " + node.getPosition()
                    );
                }

                result = left / right;
                break;

            default:
                return node;
        }

        return createNumber(result, node.getPosition());
    }

    private ExpressionNode combineAdditionConstants(ExpressionNode node) {
        List<ExpressionNode> terms = new ArrayList<>();

        collectAdditionTerms(node, terms);

        double constantSum = 0;
        boolean hasConstants = false;

        List<ExpressionNode> nonConstantTerms = new ArrayList<>();

        for (ExpressionNode term : terms) {
            if (isNumber(term)) {
                constantSum += getNumber(term);
                hasConstants = true;
            } else {
                nonConstantTerms.add(term);
            }
        }

        if (!hasConstants) {
            return node;
        }

        if (constantSum != 0 || nonConstantTerms.isEmpty()) {
            nonConstantTerms.add(createNumber(constantSum, node.getPosition()));
        }

        ExpressionNode result = nonConstantTerms.get(0);

        for (int i = 1; i < nonConstantTerms.size(); i++) {
            ExpressionNode parent = new ExpressionNode(TokenType.PLUS, "+", node.getPosition());

            parent.addChild(result);
            parent.addChild(nonConstantTerms.get(i));

            result = parent;
        }

        return result;
    }

    private void collectAdditionTerms(ExpressionNode node, List<ExpressionNode> terms) {
        if (node.getType() == TokenType.PLUS && node.getChildren().size() == 2) {

            collectAdditionTerms(node.getChildren().get(0), terms);
            collectAdditionTerms(node.getChildren().get(1), terms);

        } else {
            terms.add(node);
        }
    }

    private boolean isNumber(ExpressionNode node) {
        return node.getType() == TokenType.NUMBER;
    }

    private double getNumber(ExpressionNode node) {
        return Double.parseDouble(node.getValue());
    }

    private boolean isZero(ExpressionNode node) {
        return isNumber(node) && getNumber(node) == 0.0;
    }

    private boolean isOne(ExpressionNode node) {
        return isNumber(node) && getNumber(node) == 1.0;
    }

    private ExpressionNode createNumber(double value, int position) {
        String formattedValue;

        if (value == Math.rint(value)
                && Math.abs(value) < 1e15) {
            formattedValue = Long.toString((long) value);
        } else {
            formattedValue = Double.toString(value);
        }

        return new ExpressionNode(TokenType.NUMBER, formattedValue, position);
    }
}