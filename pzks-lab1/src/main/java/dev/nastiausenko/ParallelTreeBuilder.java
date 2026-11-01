package dev.nastiausenko;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class ParallelTreeBuilder {
    private static final Map<TokenType, String> OPERATOR_VALUES = Map.of(
            TokenType.PLUS, "+",
            TokenType.MINUS, "-",
            TokenType.MULTIPLY, "*",
            TokenType.DIVIDE, "/"
    );
    private static final Comparator<Term> BY_HEIGHT = Comparator.comparingInt(term -> term.node().getHeight());

    public ExpressionNode build(ExpressionNode node) {
        if (node == null || node.isLeaf()) {
            return node;
        }

        if (node.getType() == TokenType.PLUS || node.getType() == TokenType.MINUS) {
            return buildGroup(node, TokenType.PLUS, TokenType.MINUS);
        }

        if (node.getType() == TokenType.MULTIPLY || node.getType() == TokenType.DIVIDE) {
            return buildGroup(node, TokenType.MULTIPLY, TokenType.DIVIDE);
        }

        node.getChildren().replaceAll(this::build);

        return node;
    }

    private ExpressionNode buildGroup(ExpressionNode node, TokenType directOperator, TokenType inverseOperator) {
        List<Term> terms = new ArrayList<>();
        collectTerms(node, false, directOperator, inverseOperator, terms);
        return buildBalanced(terms, directOperator, inverseOperator, node.getPosition());
    }

    private void collectTerms(ExpressionNode node, boolean inverted, TokenType directOperator,
                              TokenType inverseOperator, List<Term> terms) {
        if (node.isBinaryOperator(directOperator)) {
            collectTerms(node.getChildren().get(0), inverted, directOperator, inverseOperator, terms);
            collectTerms(node.getChildren().get(1), inverted, directOperator, inverseOperator, terms);
            return;
        }

        if (node.isBinaryOperator(inverseOperator)) {
            collectTerms(node.getChildren().get(0), inverted, directOperator, inverseOperator, terms);
            collectTerms(node.getChildren().get(1), !inverted, directOperator, inverseOperator, terms);
            return;
        }

        if (directOperator == TokenType.PLUS && node.isUnaryMinus()) {
            collectTerms(node.getChildren().get(0), !inverted, directOperator, inverseOperator, terms);
            return;
        }

        terms.add(new Term(build(node), inverted));
    }

    private ExpressionNode buildBalanced(List<Term> terms, TokenType directOperator,
                                         TokenType inverseOperator, int position) {
        terms.sort(BY_HEIGHT);
        ExpressionNode negatedSum = null;

        if (terms.stream().allMatch(Term::inverted)) {
            negatedSum = buildNegatedSum(terms, directOperator, inverseOperator, position);
            Term first = terms.get(0);
            ExpressionNode minus = new ExpressionNode(TokenType.MINUS, "unary-", position, first.node());
            terms.set(0, new Term(minus, false));
        }

        while (terms.size() > 1) {
            sortTerms(terms, directOperator);
            Term first = terms.remove(0);
            Term second = terms.remove(0);
            terms.add(combineTerms(first, second, directOperator, inverseOperator, position));
        }

        ExpressionNode result = terms.get(0).node();

        if (negatedSum != null && negatedSum.getHeight() <= result.getHeight()
                && negatedSum.getWidth() >= result.getWidth()) {
            return negatedSum;
        }

        return result;
    }

    private ExpressionNode buildNegatedSum(List<Term> terms, TokenType directOperator,
                                           TokenType inverseOperator, int position) {
        List<Term> positiveTerms = new ArrayList<>();

        for (Term term : terms) {
            positiveTerms.add(new Term(term.node(), false));
        }

        ExpressionNode sum = buildBalanced(positiveTerms, directOperator, inverseOperator, position);
        return new ExpressionNode(TokenType.MINUS, "unary-", position, sum);
    }

    private void sortTerms(List<Term> terms, TokenType directOperator) {
        long invertedCount = terms.stream().filter(Term::inverted).count();
        boolean preferInverted = directOperator == TokenType.PLUS || invertedCount * 2 > terms.size();

        terms.sort(BY_HEIGHT.thenComparingInt(term -> term.inverted() == preferInverted ? 0 : 1));
    }

    private Term combineTerms(Term first, Term second, TokenType directOperator,
                              TokenType inverseOperator, int position) {
        if (first.inverted() == second.inverted()) {
            ExpressionNode parent = createOperator(directOperator, position, first.node(), second.node());
            return new Term(parent, first.inverted());
        }

        ExpressionNode left = first.inverted() ? second.node() : first.node();
        ExpressionNode right = first.inverted() ? first.node() : second.node();
        ExpressionNode parent = createOperator(inverseOperator, position, left, right);
        return new Term(parent, false);
    }

    private ExpressionNode createOperator(TokenType type, int position, ExpressionNode left, ExpressionNode right) {
        String value = OPERATOR_VALUES.get(type);

        if (value == null) {
            throw new IllegalArgumentException("Неочікуваний оператор: " + type);
        }

        return new ExpressionNode(type, value, position, left, right);
    }

    private record Term(ExpressionNode node, boolean inverted) { }
}
