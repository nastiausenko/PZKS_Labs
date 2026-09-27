package dev.nastiausenko;

public record LexicalError(String error, int position) {

    @Override
    public String toString() {
        return "Позиція " + position + ": " + error;
    }
}