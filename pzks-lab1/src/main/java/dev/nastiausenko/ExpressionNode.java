package dev.nastiausenko;

import java.util.ArrayList;
import java.util.List;

public class ExpressionNode {
    private final TokenType type;
    private final String value;
    private final int position;
    private final List<ExpressionNode> children;

    public ExpressionNode(TokenType type, String value, int position) {
        this.type = type;
        this.value = value;
        this.position = position;
        this.children = new ArrayList<>();
    }

    public TokenType getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public int getPosition() {
        return position;
    }

    public List<ExpressionNode> getChildren() {
        return children;
    }

    public void addChild(ExpressionNode child) {
        children.add(child);
    }

    public boolean isLeaf() {
        return children.isEmpty();
    }

    public void printTree(String indent, boolean isLast) {
        System.out.println(indent + (isLast ? "└── " : "├── ") + value);

        for (int i = 0; i < children.size(); i++) {
            String childIndent = indent + (isLast ? "    " : "│   ");
            children.get(i).printTree(
                    childIndent,
                    i == children.size() - 1
            );
        }
    }
}