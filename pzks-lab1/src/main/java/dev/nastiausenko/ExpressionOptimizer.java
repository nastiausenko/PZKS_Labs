package dev.nastiausenko;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.DoubleBinaryOperator;
import java.util.function.UnaryOperator;

public class ExpressionOptimizer {
    private static final Map<TokenType, DoubleBinaryOperator> OPERATIONS = Map.of(
            TokenType.PLUS, Double::sum,
            TokenType.MINUS, (left, right) -> left - right,
            TokenType.MULTIPLY, (left, right) -> left * right,
            TokenType.DIVIDE, (left, right) -> left / right
    );

    private final Map<TokenType, UnaryOperator<ExpressionNode>> simplifiers = Map.of(
            TokenType.PLUS, this::simplifyAddition,
            TokenType.MINUS, this::simplifySubtraction,
            TokenType.MULTIPLY, this::simplifyMultiplicationAndDivision,
            TokenType.DIVIDE, this::simplifyMultiplicationAndDivision
    );

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
                        "Ділення на 0 на позиції " + denominator.getPosition()
                );
            }
        }

        if (isArithmeticOperator(node) && hasTwoNumericChildren(node)) {
            return calculate(node);
        }

        if (isUnaryMinus(node)) {
            ExpressionNode child = node.getChildren().get(0);

            if (isNumber(child)) {
                return createNumber(-getNumber(child), node.getPosition());
            }

            if (isUnaryMinus(child)) {
                return child.getChildren().get(0);
            }

            if (child.getType() == TokenType.MINUS && child.getChildren().size() == 2) {
                ExpressionNode left = child.getChildren().get(0);
                child.getChildren().set(0, child.getChildren().get(1));
                child.getChildren().set(1, left);
                return optimize(child);
            }

            if ((child.getType() == TokenType.MULTIPLY || child.getType() == TokenType.DIVIDE)
                    && reverseDifference(child)) {
                return optimize(child);
            }
        }

        if (node.getChildren().size() == 2) {
            ExpressionNode left = node.getChildren().get(0);
            ExpressionNode right = node.getChildren().get(1);

            if (node.getType() == TokenType.PLUS && isUnaryMinus(left) && isUnaryMinus(right)) {
                node.getChildren().set(0, left.getChildren().get(0));
                node.getChildren().set(1, right.getChildren().get(0));

                ExpressionNode result = optimize(node);
                ExpressionNode minus = new ExpressionNode(TokenType.MINUS, "unary-", node.getPosition());
                minus.addChild(result);
                return optimize(minus);
            }

            return simplifiers.getOrDefault(node.getType(), UnaryOperator.identity()).apply(node);
        }

        return node;
    }

    private ExpressionNode simplifyAddition(ExpressionNode node) {
        ExpressionNode left = node.getChildren().get(0);
        ExpressionNode right = node.getChildren().get(1);

        if (isZero(left)) {
            return right;
        }

        if (isZero(right)) {
            return left;
        }

        if (isUnaryMinus(left) || isUnaryMinus(right)) {
            ExpressionNode negativeTerm = isUnaryMinus(left) ? left : right;
            ExpressionNode positiveTerm = isUnaryMinus(left) ? right : left;
            ExpressionNode subtraction = new ExpressionNode(TokenType.MINUS, "-", node.getPosition());
            subtraction.addChild(positiveTerm);
            subtraction.addChild(negativeTerm.getChildren().get(0));
            return optimize(subtraction);
        }

        return combineAdditionConstants(node);
    }

    private ExpressionNode simplifySubtraction(ExpressionNode node) {
        if (isZero(node.getChildren().get(1))) {
            return node.getChildren().get(0);
        }

        return node;
    }

    private boolean reverseDifference(ExpressionNode node) {
        if (node.getType() == TokenType.MINUS && node.getChildren().size() == 2) {
            ExpressionNode left = node.getChildren().get(0);
            node.getChildren().set(0, node.getChildren().get(1));
            node.getChildren().set(1, left);
            return true;
        }

        if (node.getType() == TokenType.MULTIPLY || node.getType() == TokenType.DIVIDE) {
            for (ExpressionNode child : node.getChildren()) {
                if (reverseDifference(child)) {
                    return true;
                }
            }
        }

        return false;
    }

    private ExpressionNode simplifyMultiplicationAndDivision(ExpressionNode node) {
        boolean negative = false;

        for (int i = 0; i < node.getChildren().size(); i++) {
            ExpressionNode child = node.getChildren().get(i);

            if (isUnaryMinus(child)) {
                child = child.getChildren().get(0);
                negative = !negative;
            } else if (isNumber(child) && getNumber(child) < 0) {
                child = createNumber(-getNumber(child), child.getPosition());
                negative = !negative;
            }

            node.getChildren().set(i, child);
        }

        ExpressionNode left = node.getChildren().get(0);
        ExpressionNode right = node.getChildren().get(1);

        if (node.getType() == TokenType.MULTIPLY && (isZero(left) || isZero(right))) {
            return createNumber(0, node.getPosition());
        }

        ExpressionNode result = node;

        if (isOne(right)) {
            result = left;
        } else if (node.getType() == TokenType.MULTIPLY && isOne(left)) {
            result = right;
        }

        if (negative) {
            ExpressionNode minus = new ExpressionNode(TokenType.MINUS, "unary-", node.getPosition());
            minus.addChild(result);
            return optimize(minus);
        }

        return result;
    }

    private boolean isArithmeticOperator(ExpressionNode node) {
        return OPERATIONS.containsKey(node.getType());
    }

    private boolean hasTwoNumericChildren(ExpressionNode node) {
        return node.getChildren().size() == 2
                && isNumber(node.getChildren().get(0))
                && isNumber(node.getChildren().get(1));
    }

    private ExpressionNode calculate(ExpressionNode node) {
        double left = getNumber(node.getChildren().get(0));
        double right = getNumber(node.getChildren().get(1));

        if (node.getType() == TokenType.DIVIDE && right == 0.0) {
            throw new ArithmeticException(
                    "Ділення на 0 на позиції " + node.getChildren().get(1).getPosition()
            );
        }

        double result = OPERATIONS.get(node.getType()).applyAsDouble(left, right);
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

    private boolean isUnaryMinus(ExpressionNode node) {
        return "unary-".equals(node.getValue()) && node.getChildren().size() == 1;
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