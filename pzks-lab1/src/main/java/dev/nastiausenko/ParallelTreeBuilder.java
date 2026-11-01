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
            collectTerms(node.getChildren().getFirst(), !inverted, directOperator, inverseOperator, terms);

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

        if (allInverted) {
            Term first = terms.getFirst();
            ExpressionNode minus = new ExpressionNode(TokenType.MINUS, "unary-", position);
            minus.addChild(first.node());
            terms.set(0, new Term(minus, false));
        }

        while (terms.size() > 1) {
            // Спочатку об'єднуємо підвирази з найменшою висотою.
            // За однакової висоти надаємо перевагу від'ємним доданкам або знаменникам.
            terms.sort(Comparator.comparingInt((Term term) -> term.node().getHeight())
                    .thenComparingInt(term -> term.inverted() ? 0 : 1));

            Term first = terms.removeFirst();
            Term second = terms.removeFirst();

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

        return terms.getFirst().node();
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

    // inverted позначає від'ємний доданок або множник у знаменнику.
    private record Term(ExpressionNode node, boolean inverted) {
    }
}
