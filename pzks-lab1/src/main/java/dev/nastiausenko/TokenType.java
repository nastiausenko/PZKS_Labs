package dev.nastiausenko;

public enum TokenType {
    NUMBER("числом"),
    IDENTIFIER("ідентифікатором"),

    PLUS("+", "оператором"),
    MINUS("-", "оператором"),
    MULTIPLY("*", "оператором"),
    DIVIDE("/", "оператором"),

    LEFT_PAREN("(", "відкриваючою дужкою"),
    RIGHT_PAREN(")", "закриваючою дужкою"),
    COMMA(",", "кома"),

    FUNCTION(new String[]{"sin", "cos", "tan", "asin", "acos", "atan",
                    "sqrt", "abs", "log", "ln", "exp", "pow"},
            "функцією"),

    CONSTANT(new String[]{"PI", "E"},
            "константою"),

    END("кінець виразу");

    private final String[] values;
    private final String description;

    TokenType(String description) {
        this.values = new String[0];
        this.description = description;
    }

    TokenType(String value, String description) {
        this.values = new String[]{value};
        this.description = description;
    }

    TokenType(String[] values, String description) {
        this.values = values;
        this.description = description;
    }

    public boolean hasValue(String value) {
        for (String item : values) {
            if (item.equals(value)) {
                return true;
            }
        }
        return false;
    }

    public boolean isFunction(String value) {
        return hasValue(value) || value.matches("f\\d+");
    }

    public String getDescription() {
        return description;
    }
}