package dev.nastiausenko;

public class SyntaxError {
    private final String error;
    private final int position;

    public SyntaxError(String error, int position) {
        this.error = error;
        this.position = position;
    }

    @Override
    public String toString() {
        return "Позиція " + position + ": " + error;
    }
}
