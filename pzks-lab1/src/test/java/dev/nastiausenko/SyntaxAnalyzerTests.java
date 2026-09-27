package dev.nastiausenko;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SyntaxAnalyzerTests {

    private final Lexer lexer = new Lexer();
    private final SyntaxAnalyzer analyzer = new SyntaxAnalyzer();

    private List<SyntaxError> analyze(String expression) {
        List<Token> tokens = lexer.tokenize(expression);

        assertTrue(lexer.getErrors().isEmpty(), "Лексичні помилки у тестовому виразі: " + lexer.getErrors());

        return analyzer.analyze(tokens);
    }

    @Test
    void shouldAcceptSimpleExpression() {
        List<SyntaxError> errors = analyze("a + b * 2");

        assertTrue(errors.isEmpty());
    }

    @Test
    void shouldAcceptNumberExpression() {
        List<SyntaxError> errors = analyze("12.5 + 3.14 * 2");

        assertTrue(errors.isEmpty());
    }

    @Test
    void shouldAcceptExpressionWithParentheses() {
        List<SyntaxError> errors = analyze("(a + b) * (c - d)");

        assertTrue(errors.isEmpty());
    }

    @Test
    void shouldAcceptNestedFunctions() {
        List<SyntaxError> errors = analyze("sqrt(abs(sin(x + cos(y))))");

        assertTrue(errors.isEmpty());
    }

    @Test
    void shouldAcceptPowWithTwoArguments() {
        List<SyntaxError> errors = analyze("pow(x, 2)");

        assertTrue(errors.isEmpty());
    }

    @Test
    void shouldAcceptCustomFunctionWithOneArgument() {
        List<SyntaxError> errors = analyze("f1(x)");

        assertTrue(errors.isEmpty());
    }

    @Test
    void shouldAcceptCustomFunctionWithTwoArguments() {
        List<SyntaxError> errors = analyze("f123(x, y)");

        assertTrue(errors.isEmpty());
    }

    @Test
    void shouldAcceptNegativeNumberAtBeginning() {
        List<SyntaxError> errors = analyze("-5 + x");

        assertTrue(errors.isEmpty());
    }

    @Test
    void shouldAcceptNegativeOperandInParentheses() {
        List<SyntaxError> errors = analyze("x + (-5)");

        assertTrue(errors.isEmpty());
    }

    @Test
    void shouldAcceptNegativeIdentifierInParentheses() {
        List<SyntaxError> errors = analyze("x * (-y)");

        assertTrue(errors.isEmpty());
    }

    @Test
    void shouldAcceptNegativeFunctionInParentheses() {
        List<SyntaxError> errors = analyze("x + (-f1(y))");

        assertTrue(errors.isEmpty());
    }

    @Test
    void shouldRejectMissingOperatorBetweenOperands() {
        List<SyntaxError> errors = analyze("x 45.98");

        assertFalse(errors.isEmpty());

        assertTrue(errors.get(0).getError().contains("відсутній оператор"));
    }

    @Test
    void shouldRejectMissingOperatorBeforeParenthesis() {
        List<SyntaxError> errors = analyze("45.98(x)");

        assertFalse(errors.isEmpty());

        assertTrue(errors.get(0).getError().contains("відсутній оператор"));
    }

    @Test
    void shouldRejectOperatorAtBeginning() {
        List<SyntaxError> errors = analyze("*x");

        assertFalse(errors.isEmpty());
    }

    @Test
    void shouldRejectTwoOperatorsInARow() {
        List<SyntaxError> errors = analyze("x + * y");

        assertFalse(errors.isEmpty());

        assertTrue(errors.get(0).getError().contains("Неочікуваний"));
    }

    @Test
    void shouldRejectUnclosedParenthesis() {
        List<SyntaxError> errors = analyze("(x + y");

        assertFalse(errors.isEmpty());

        assertTrue(errors.stream().anyMatch(error -> error.getError().contains("закриваючої дужки")));
    }

    @Test
    void shouldRejectUnexpectedClosingParenthesis() {
        List<SyntaxError> errors = analyze("x + y)");

        assertFalse(errors.isEmpty());

        assertTrue(errors.stream().anyMatch(error -> error.getError().contains("відповідної відкриваючої")));
    }

    @Test
    void shouldRejectEmptyParentheses() {
        List<SyntaxError> errors = analyze("()");

        assertFalse(errors.isEmpty());

        assertTrue(errors.stream().anyMatch(error -> error.getError().contains("Порожні дужки")));
    }

    @Test
    void shouldRejectFunctionWithoutParentheses() {
        List<SyntaxError> errors = analyze("sin");

        assertFalse(errors.isEmpty());

        assertTrue(errors.stream().anyMatch(error -> error.getError().contains("очікувалася відкриваюча дужка"))
        );
    }

    @Test
    void shouldRejectPowWithOneArgument() {
        List<SyntaxError> errors = analyze("pow(x)");

        assertFalse(errors.isEmpty());

        assertTrue(errors.stream().anyMatch(error -> error.getError().contains("рівно 2 аргументи")));
    }

    @Test
    void shouldRejectPowWithThreeArguments() {
        List<SyntaxError> errors = analyze("pow(x, y, z)");

        assertFalse(errors.isEmpty());

        assertTrue(errors.stream().anyMatch(error -> error.getError().contains("більше 2 аргументів")));
    }

    @Test
    void shouldRejectFunctionWithTooManyArguments() {
        List<SyntaxError> errors = analyze("f1(x, y, z)");

        assertFalse(errors.isEmpty());

        assertTrue(errors.stream().anyMatch(error -> error.getError().contains("більше 2 аргументів")));
    }

    @Test
    void shouldRejectStandardFunctionWithTwoArguments() {
        List<SyntaxError> errors = analyze("sin(x, y)");

        assertFalse(errors.isEmpty());

        assertTrue(errors.stream().anyMatch(error -> error.getError().contains("лише один аргумент")));
    }

    @Test
    void shouldRejectCommaOutsideFunction() {
        List<SyntaxError> errors = analyze("x, y");

        assertFalse(errors.isEmpty());

        assertTrue(errors.stream().anyMatch(error -> error.getError().contains("поза функцією")));
    }

    @Test
    void shouldRejectCommaWithoutArgumentBeforeIt() {
        List<SyntaxError> errors = analyze("pow(, x)");

        assertFalse(errors.isEmpty());

        assertTrue(errors.stream().anyMatch(error -> error.getError().contains("Перед комою")));
    }

    @Test
    void shouldRejectCommaWithoutArgumentAfterIt() {
        List<SyntaxError> errors = analyze("pow(x,)");

        assertFalse(errors.isEmpty());

        assertTrue(errors.stream().anyMatch(error -> error.getError().contains("Перед закриваючою")));
    }

    @Test
    void shouldRejectNegativeOperandWithoutParentheses() {
        List<SyntaxError> errors = analyze("a + -5");

        assertFalse(errors.isEmpty());

        assertTrue(errors.stream().anyMatch(error -> error.getError().contains("Від'ємний операнд у середині виразу")));
    }

    @Test
    void shouldRejectNegativeIdentifierWithoutParentheses() {
        List<SyntaxError> errors = analyze("a * -x");

        assertFalse(errors.isEmpty());

        assertTrue(errors.stream().anyMatch(error -> error.getError().contains("Від'ємний операнд у середині виразу")));
    }

    @Test
    void shouldRejectNegativeFunctionWithoutParentheses() {
        List<SyntaxError> errors = analyze("a + -f1(x)");

        assertFalse(errors.isEmpty());

        assertTrue(errors.stream().anyMatch(error -> error.getError().contains("Від'ємний операнд у середині виразу")));
    }

    @Test
    void shouldAcceptLargeValidExpression() {
        List<SyntaxError> errors = analyze(
                "sqrt(pow(sin(x + cos(y)), 2) + pow(cos(z - PI), 2)) /(abs(a - b) + exp(-c))"
        );

        assertTrue(errors.isEmpty());
    }

    @Test
    void shouldRejectExpressionEndingWithOperator() {
        List<SyntaxError> errors = analyze("x +");

        assertFalse(errors.isEmpty());

        assertTrue(errors.stream().anyMatch(error -> error.getError().contains("Вираз закінчується")));
    }
}