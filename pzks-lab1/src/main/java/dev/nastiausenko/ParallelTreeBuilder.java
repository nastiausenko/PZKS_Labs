package dev.nastiausenko;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ParallelTreeBuilder {

    public ExpressionNode build(ExpressionNode node) {
        if (node == null || node.isLeaf()) {
            return node;
        }

        if (node.getType() == TokenType.PLUS || node.getType() == TokenType.MINUS) {
            List<Term> terms = new ArrayList<>();
            collectTerms(node, false, TokenType.PLUS, TokenType.MINUS, terms);

            return buildBalanced(terms, TokenType.PLUS, TokenType.MINUS, node.getPosition());
        }

        if (node.getType() == TokenType.MULTIPLY || node.getType() == TokenType.DIVIDE) {
            List<Term> terms = new ArrayList<>();
            collectTerms(node, false, TokenType.MULTIPLY, TokenType.DIVIDE, terms);

            return buildBalanced(terms, TokenType.MULTIPLY, TokenType.DIVIDE, node.getPosition());
        }

        node.getChildren().replaceAll(this::build);

        return node;
    }

    private void collectTerms(ExpressionNode node, boolean inverted, TokenType directOperator,
                              TokenType inverseOperator, List<Term> terms) {
        if (node.getType() == directOperator && node.getChildren().size() == 2) {
            collectTerms(node.getChildren().get(0), inverted, directOperator, inverseOperator, terms);
            collectTerms(node.getChildren().get(1), inverted, directOperator, inverseOperator, terms);

        } else if (node.getType() == inverseOperator && node.getChildren().size() == 2) {
            collectTerms(node.getChildren().get(0), inverted, directOperator, inverseOperator, terms);
            collectTerms(node.getChildren().get(1), !inverted, directOperator, inverseOperator, terms);

        } else if (directOperator == TokenType.PLUS && "unary-".equals(node.getValue())) {
            collectTerms(node.getChildren().get(0), !inverted, directOperator, inverseOperator, terms);

        } else {
            terms.add(new Term(build(node), inverted));
        }
    }

    private ExpressionNode buildBalanced(List<Term> terms, TokenType directOperator,
                                         TokenType inverseOperator, int position) {
        terms.sort(Comparator.comparingInt((Term term) -> term.node().getHeight()));

        boolean allInverted = true;

        for (Term term : terms) {
            if (!term.inverted()) {
                allInverted = false;
                break;
            }
        }

        ExpressionNode negatedSum = null;

        if (allInverted) {
            List<Term> positiveTerms = new ArrayList<>();

            for (Term term : terms) {
                positiveTerms.add(new Term(term.node(), false));
            }

            negatedSum = new ExpressionNode(TokenType.MINUS, "unary-", position);
            negatedSum.addChild(buildBalanced(positiveTerms, directOperator, inverseOperator, position));

            Term first = terms.get(0);
            ExpressionNode minus = new ExpressionNode(TokenType.MINUS, "unary-", position);
            minus.addChild(first.node());
            terms.set(0, new Term(minus, false));
        }

        while (terms.size() > 1) {
            int invertedCount = 0;

            for (Term term : terms) {
                if (term.inverted()) {
                    invertedCount++;
                }
            }

            boolean preferInverted = directOperator == TokenType.PLUS || invertedCount * 2 > terms.size();

            terms.sort(Comparator.comparingInt((Term term) -> term.node().getHeight())
                    .thenComparingInt(term -> term.inverted() == preferInverted ? 0 : 1));

            Term first = terms.remove(0);
            Term second = terms.remove(0);

            ExpressionNode parent;
            boolean inverted;

            if (first.inverted() == second.inverted()) {
                parent = createOperator(directOperator, position);
                parent.addChild(first.node());
                parent.addChild(second.node());
                inverted = first.inverted();
            } else {
                parent = createOperator(inverseOperator, position);
                parent.addChild(first.inverted() ? second.node() : first.node());
                parent.addChild(first.inverted() ? first.node() : second.node());
                inverted = false;
            }

            terms.add(new Term(parent, inverted));
        }

        ExpressionNode result = terms.get(0).node();

        if (negatedSum != null && negatedSum.getHeight() <= result.getHeight()
                && negatedSum.getWidth() >= result.getWidth()) {
            return negatedSum;
        }

        return result;
    }

    private ExpressionNode createOperator(TokenType type, int position) {
        String value = switch (type) {
            case PLUS -> "+";
            case MINUS -> "-";
            case MULTIPLY -> "*";
            case DIVIDE -> "/";
            default -> throw new IllegalArgumentException("Неочікуваний оператор: " + type);
        };

        return new ExpressionNode(type, value, position);
    }

    private record Term(ExpressionNode node, boolean inverted) {
    }
}
