package dev.nastiausenko;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LexerTests {

    private final Lexer lexer = new Lexer();

    @Test
    void shouldRecognizeNumbers() {
        List<Token> tokens = lexer.tokenize("0 12 3.14 0.5 123.456");

        assertEquals(5, tokens.size() - 1);

        assertEquals(TokenType.NUMBER, tokens.get(0).getType());
        assertEquals("0", tokens.get(0).getValue());

        assertEquals(TokenType.NUMBER, tokens.get(1).getType());
        assertEquals("12", tokens.get(1).getValue());

        assertEquals(TokenType.NUMBER, tokens.get(2).getType());
        assertEquals("3.14", tokens.get(2).getValue());

        assertEquals(TokenType.NUMBER, tokens.get(3).getType());
        assertEquals("0.5", tokens.get(3).getValue());

        assertEquals(TokenType.NUMBER, tokens.get(4).getType());
        assertEquals("123.456", tokens.get(4).getValue());
    }

    @Test
    void shouldRecognizeIdentifiers() {
        List<Token> tokens = lexer.tokenize("x abc a1 test_123");

        assertEquals(TokenType.IDENTIFIER, tokens.get(0).getType());
        assertEquals("x", tokens.get(0).getValue());

        assertEquals(TokenType.IDENTIFIER, tokens.get(1).getType());
        assertEquals("abc", tokens.get(1).getValue());

        assertEquals(TokenType.IDENTIFIER, tokens.get(2).getType());
        assertEquals("a1", tokens.get(2).getValue());

        assertEquals(TokenType.IDENTIFIER, tokens.get(3).getType());
        assertEquals("test_123", tokens.get(3).getValue());
    }

    @Test
    void shouldRecognizeOperatorsAndParentheses() {
        List<Token> tokens = lexer.tokenize("a+b-c*d/e");

        assertEquals(TokenType.IDENTIFIER, tokens.get(0).getType());
        assertEquals(TokenType.PLUS, tokens.get(1).getType());
        assertEquals(TokenType.IDENTIFIER, tokens.get(2).getType());
        assertEquals(TokenType.MINUS, tokens.get(3).getType());
        assertEquals(TokenType.IDENTIFIER, tokens.get(4).getType());
        assertEquals(TokenType.MULTIPLY, tokens.get(5).getType());
        assertEquals(TokenType.IDENTIFIER, tokens.get(6).getType());
        assertEquals(TokenType.DIVIDE, tokens.get(7).getType());
        assertEquals(TokenType.IDENTIFIER, tokens.get(8).getType());
    }

    @Test
    void shouldRecognizeFunctions() {
        List<Token> tokens = lexer.tokenize(
                "sin(x) cos(x) tan(x) sqrt(x) abs(x) log(x) ln(x) exp(x) pow(x,y)"
        );

        assertEquals(TokenType.FUNCTION, tokens.get(0).getType());
        assertEquals("sin", tokens.get(0).getValue());

        assertEquals(TokenType.FUNCTION, tokens.get(4).getType());
        assertEquals("cos", tokens.get(4).getValue());

        assertEquals(TokenType.FUNCTION, tokens.get(8).getType());
        assertEquals("tan", tokens.get(8).getValue());

        assertEquals(TokenType.FUNCTION, tokens.get(12).getType());
        assertEquals("sqrt", tokens.get(12).getValue());

        assertEquals(TokenType.FUNCTION, tokens.get(16).getType());
        assertEquals("abs", tokens.get(16).getValue());

        assertEquals(TokenType.FUNCTION, tokens.get(20).getType());
        assertEquals("log", tokens.get(20).getValue());

        assertEquals(TokenType.FUNCTION, tokens.get(24).getType());
        assertEquals("ln", tokens.get(24).getValue());

        assertEquals(TokenType.FUNCTION, tokens.get(28).getType());
        assertEquals("exp", tokens.get(28).getValue());

        assertEquals(TokenType.FUNCTION, tokens.get(32).getType());
        assertEquals("pow", tokens.get(32).getValue());
    }

    @Test
    void shouldRecognizeCustomFunctions() {
        List<Token> tokens = lexer.tokenize("f1(x) f2(x,y) f123(a,b)");

        assertEquals(TokenType.FUNCTION, tokens.get(0).getType());
        assertEquals("f1", tokens.get(0).getValue());

        assertEquals(TokenType.FUNCTION, tokens.get(4).getType());
        assertEquals("f2", tokens.get(4).getValue());

        assertEquals(TokenType.FUNCTION, tokens.get(10).getType());
        assertEquals("f123", tokens.get(10).getValue());
    }

    @Test
    void shouldRecognizeConstants() {
        List<Token> tokens = lexer.tokenize("PI + E");

        assertEquals(TokenType.CONSTANT, tokens.get(0).getType());
        assertEquals("PI", tokens.get(0).getValue());

        assertEquals(TokenType.CONSTANT, tokens.get(2).getType());
        assertEquals("E", tokens.get(2).getValue());
    }

    @Test
    void shouldRecognizeComma() {
        List<Token> tokens = lexer.tokenize("pow(x, 2)");

        assertEquals(TokenType.COMMA, tokens.get(3).getType());
        assertEquals(",", tokens.get(3).getValue());
    }

    @Test
    void shouldIgnoreSpaces() {
        List<Token> tokens = lexer.tokenize("  x +  2.5 ");

        assertEquals(TokenType.IDENTIFIER, tokens.get(0).getType());
        assertEquals(TokenType.PLUS, tokens.get(1).getType());
        assertEquals(TokenType.NUMBER, tokens.get(2).getType());
        assertEquals("2.5", tokens.get(2).getValue());
    }

    @Test
    void shouldReportUnknownCharacters() {
        lexer.tokenize("x + a%");

        List<LexicalError> errors = lexer.getErrors();

        assertEquals(1, errors.size());
        assertEquals(5, errors.get(0).position());
        assertTrue(errors.get(0).error().contains("Невідомий символ"));
    }

    @Test
    void shouldReportMultipleUnknownCharacters() {
        lexer.tokenize("a@b#c$");

        List<LexicalError> errors = lexer.getErrors();

        assertEquals(3, errors.size());

        assertEquals(1, errors.get(0).position());
        assertEquals(3, errors.get(1).position());
        assertEquals(5, errors.get(2).position());
    }

    @Test
    void shouldAddEndToken() {
        List<Token> tokens = lexer.tokenize("x + 1");

        Token endToken = tokens.get(tokens.size() - 1);

        assertEquals(TokenType.END, endToken.getType());
    }

    @Test
    void shouldAcceptComplexExpression() {
        List<Token> tokens = lexer.tokenize("sqrt(pow(x + f1(y), 2) + sin(z)) / PI");

        assertTrue(lexer.getErrors().isEmpty());
        assertEquals(TokenType.END, tokens.get(tokens.size() - 1).getType());
    }
}