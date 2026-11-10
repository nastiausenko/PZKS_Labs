package dev.nastiausenko.expression;

import dev.nastiausenko.lexer.TokenType;

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

        node.getChildren().replaceAll(this::optimize);
        validateDivision(node);

        if (isArithmeticOperator(node) && hasTwoNumericChildren(node)) {
            return calculate(node);
        }

        if (node.isUnaryMinus()) {
            return simplifyUnaryMinus(node);
        }

        if (node.getChildren().size() == 2) {
            return simplifiers.getOrDefault(node.getType(), UnaryOperator.identity()).apply(node);
        }

        return node;
    }

    private void validateDivision(ExpressionNode node) {
        if (node.getType() != TokenType.DIVIDE) {
            return;
        }

        ExpressionNode denominator = node.getChildren().get(1);

        if (isZero(denominator)) {
            throw new ArithmeticException("Ділення на 0 на позиції " + denominator.getPosition());
        }
    }

    private ExpressionNode simplifyUnaryMinus(ExpressionNode node) {
        ExpressionNode operand = node.getChildren().get(0);

        if (isNumber(operand)) {
            return createNumber(-getNumber(operand), node.getPosition());
        }

        if (operand.isUnaryMinus()) {
            return operand.getChildren().get(0);
        }

        if (reverseFirstDifference(operand)) {
            return optimize(operand);
        }

        return node;
    }

    private ExpressionNode simplifyAddition(ExpressionNode node) {
        ExpressionNode left = node.getChildren().get(0);
        ExpressionNode right = node.getChildren().get(1);

        if (left.isUnaryMinus() && right.isUnaryMinus()) {
            node.getChildren().set(0, left.getChildren().get(0));
            node.getChildren().set(1, right.getChildren().get(0));
            return negate(optimize(node), node.getPosition());
        }

        if (isZero(left)) {
            return right;
        }

        if (isZero(right)) {
            return left;
        }

        if (left.isUnaryMinus() || right.isUnaryMinus()) {
            ExpressionNode negativeTerm = left.isUnaryMinus() ? left : right;
            ExpressionNode positiveTerm = left.isUnaryMinus() ? right : left;
            ExpressionNode subtraction = new ExpressionNode(
                    TokenType.MINUS, "-", node.getPosition(),
                    positiveTerm, negativeTerm.getChildren().get(0)
            );
            return optimize(subtraction);
        }

        return combineAdditionConstants(node);
    }

    private ExpressionNode simplifySubtraction(ExpressionNode node) {
        ExpressionNode left = node.getChildren().get(0);
        ExpressionNode right = node.getChildren().get(1);

        if (isSameExpression(left, right)) {
            return createNumber(0, node.getPosition());
        }

        if (isZero(right)) {
            return left;
        }

        return node;
    }

    private boolean isSameExpression(ExpressionNode left, ExpressionNode right) {
        if (left.getType() != right.getType() || !left.getValue().equals(right.getValue())
                || left.getChildren().size() != right.getChildren().size()) {
            return false;
        }

        for (int i = 0; i < left.getChildren().size(); i++) {
            if (!isSameExpression(left.getChildren().get(i), right.getChildren().get(i))) {
                return false;
            }
        }

        return true;
    }

    private boolean reverseFirstDifference(ExpressionNode node) {
        if (node.isBinaryOperator(TokenType.MINUS)) {
            ExpressionNode left = node.getChildren().get(0);
            ExpressionNode right = node.getChildren().get(1);
            node.getChildren().set(0, right);
            node.getChildren().set(1, left);
            return true;
        }

        if (node.getType() == TokenType.MULTIPLY || node.getType() == TokenType.DIVIDE) {
            for (ExpressionNode child : node.getChildren()) {
                if (reverseFirstDifference(child)) {
                    return true;
                }
            }
        }

        return false;
    }

    private ExpressionNode simplifyMultiplicationAndDivision(ExpressionNode node) {
        boolean negative = removeFactorSigns(node);
        boolean multiplication = node.getType() == TokenType.MULTIPLY;
        ExpressionNode left = node.getChildren().get(0);
        ExpressionNode right = node.getChildren().get(1);

        if (multiplication && (isZero(left) || isZero(right))) {
            return createNumber(0, node.getPosition());
        }

        ExpressionNode result = node;

        if (isOne(right)) {
            result = left;
        } else if (multiplication && isOne(left)) {
            result = right;
        }

        return negative ? negate(result, node.getPosition()) : result;
    }

    private boolean removeFactorSigns(ExpressionNode node) {
        boolean negative = false;

        for (int i = 0; i < node.getChildren().size(); i++) {
            ExpressionNode child = node.getChildren().get(i);

            if (child.isUnaryMinus()) {
                child = child.getChildren().get(0);
                negative = !negative;
            } else if (isNumber(child) && getNumber(child) < 0) {
                child = createNumber(-getNumber(child), child.getPosition());
                negative = !negative;
            }

            node.getChildren().set(i, child);
        }

        return negative;
    }

    private ExpressionNode negate(ExpressionNode node, int position) {
        return optimize(new ExpressionNode(TokenType.MINUS, "unary-", position, node));
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
            result = new ExpressionNode(TokenType.PLUS, "+", node.getPosition(), result, nonConstantTerms.get(i));
        }

        return result;
    }

    private void collectAdditionTerms(ExpressionNode node, List<ExpressionNode> terms) {
        if (node.isBinaryOperator(TokenType.PLUS)) {
            collectAdditionTerms(node.getChildren().get(0), terms);
            collectAdditionTerms(node.getChildren().get(1), terms);
            return;
        }

        terms.add(node);
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