package dev.nastiausenko;

import java.util.ArrayList;
import java.util.List;

public class Lexer {
    public List<Token> tokenize(String input) {
        List<Token> tokens = new ArrayList<>();
        int position = 0;

        while (position < input.length()) {
            char current = input.charAt(position);

            if (Character.isWhitespace(current)) {
                position++;
                continue;
            }

            if (Character.isDigit(current)) {
                position = readNumber(input, position, tokens);
                continue;
            }

            if (Character.isLetter(current)) {
                position = readLetter(input, position, tokens);
                continue;
            }

            String symbol = String.valueOf(current);
            TokenType tokenType = findTokenType(symbol);

            if (tokenType == null) {
                System.out.println("Невідомий символ " + symbol + " на позиції " + position);
            } else {
                tokens.add(new Token(tokenType, symbol, position));
            }

            position++;
        }
        return tokens;
    }

    private TokenType findTokenType(String symbol) {
        for (TokenType tokenType : TokenType.values()) {
            if (tokenType.hasValue(symbol)) {
                return tokenType;
            }
        }
        return null;
    }

    private int readLetter(String input, int position, List<Token> tokens) {
        int startPosition = position;
        position++;

        while (position < input.length()) {
            char current = input.charAt(position);

            if (Character.isLetterOrDigit(current) || current == '_') {
                position++;
            } else {
                break;
            }
        }

        String value = input.substring(startPosition, position);

        TokenType tokenType;
        if (TokenType.FUNCTION.hasValue(value)) {
            tokenType = TokenType.FUNCTION;
        } else if (TokenType.CONSTANT.hasValue(value)) {
            tokenType = TokenType.CONSTANT;
        } else {
            tokenType = TokenType.IDENTIFIER;
        }

        tokens.add(new Token(tokenType, value, startPosition));
        return position;
    }

    private int readNumber(String input, int position, List<Token> tokens) {
        int startPosition = position;
        while (position < input.length() && Character.isDigit(input.charAt(position))) {
            position++;
        }

        if (position < input.length() && input.charAt(position) == '.') {
            position++;

            if (position >= input.length() || !Character.isDigit(input.charAt(position))) {
                System.out.println("Некоректне число на позиції" + position);
                position++;
            }

            while (position < input.length() && Character.isDigit(input.charAt(position))) {
                position++;
            }
        }

        String value = input.substring(startPosition, position);
        tokens.add(new Token(TokenType.NUMBER, value, startPosition));
        return position;
    }
}
