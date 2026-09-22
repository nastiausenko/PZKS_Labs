package dev.nastiausenko;

public enum TokenType {
    NUMBER,
    IDENTIFIER,

    PLUS("+"),
    MINUS("-"),
    MULTIPLY("*"),
    DIVIDE("/"),

    LEFT_PAREN("("),
    RIGHT_PAREN(")"),
    COMMA(","),

    FUNCTION("sin", "cos", "tan", "asin", "acos", "atan",
            "sqrt", "abs", "log", "ln", "exp", "pow"),

    CONSTANT("PI", "E"),

    END;

    private final String[] values;

    TokenType(String... values) {
        this.values = values;
    }

    TokenType() {
        this.values = new String[0];
    }

    public boolean hasValue(String value) {
        for (String item : values) {
            if (item.equals(value)) {
                return true;
            }
        }
        return false;
    }
}