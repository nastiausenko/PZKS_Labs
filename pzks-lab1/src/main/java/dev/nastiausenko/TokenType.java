package dev.nastiausenko;

public enum TokenType {
    NUMBER("число"),
    IDENTIFIER("ідентифікатор"),

    PLUS("+", "оператор"),
    MINUS("-", "оператор"),
    MULTIPLY("*", "оператор"),
    DIVIDE("/", "оператор"),

    LEFT_PAREN("(", "відкриваюча дужка"),
    RIGHT_PAREN(")", "закриваюча дужка"),
    COMMA(",", "кома"),

    FUNCTION(new String[]{"sin", "cos", "tan", "asin", "acos", "atan",
                    "sqrt", "abs", "log", "ln", "exp", "pow"},
            "функція"),

    CONSTANT(new String[]{"PI", "E"},
            "константа"),

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