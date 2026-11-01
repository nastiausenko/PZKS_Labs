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

    public int getOperationCount() {
        int count = isLeaf() ? 0 : 1;

        for (ExpressionNode child : children) {
            count += child.getOperationCount();
        }

        return count;
    }

    public int getHeight() {
        int height = 0;

        for (ExpressionNode child : children) {
            height = Math.max(height, child.getHeight() + 1);
        }

        return height;
    }

    public int getWidth() {
        int width = 0;

        for (List<ExpressionNode> level : getLevels()) {
            width = Math.max(width, level.size());
        }

        return width;
    }

    public List<List<ExpressionNode>> getLevels() {
        List<List<ExpressionNode>> levels = new ArrayList<>();
        collectLevels(levels);
        return levels;
    }

    private int collectLevels(List<List<ExpressionNode>> levels) {
        int height = 0;

        for (ExpressionNode child : children) {
            height = Math.max(height, child.collectLevels(levels) + 1);
        }

        if (!isLeaf()) {
            while (levels.size() < height) {
                levels.add(new ArrayList<>());
            }

            levels.get(height - 1).add(this);
        }

        return height;
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
