package dev.nastiausenko;

import java.util.*;

public class SyntaxAnalyzer {
    private enum State {
        EXPECT_OPERAND,
        EXPECT_OPERATOR,
        EXPECT_FUNCTION_PAREN
    }

    private final Map<State, Map<TokenType, State>> transitions = new HashMap<>();

    private boolean isOperand(TokenType type) {
        return type == TokenType.NUMBER
                || type == TokenType.IDENTIFIER
                || type == TokenType.CONSTANT
                || type == TokenType.FUNCTION;
    }

    public SyntaxAnalyzer() {
        initializeTransitions();
    }

    public List<SyntaxError> analyze(List<Token> tokens) {
        List<SyntaxError> errors = new ArrayList<>();

        State state = State.EXPECT_OPERAND;
        Deque<Context> contextStack = new ArrayDeque<>();
        String pendingFunction = null;
        Token previousToken = null;

        for (Token token : tokens) {
            TokenType tokenType = token.getType();

            if (tokenType == TokenType.END) {
                if (state == State.EXPECT_OPERAND && previousToken != null) {
                    errors.add(new SyntaxError(
                            "Вираз закінчується " + previousToken.getType().getDescription() + " '"
                                    + previousToken.getValue() + "'. Після нього повинен бути операнд.",
                            token.getPosition()
                    ));
                }

                if (state == State.EXPECT_FUNCTION_PAREN) {
                    errors.add(new SyntaxError(
                            "Після функції '" + pendingFunction + "' очікувалася відкриваюча дужка '('.",
                            token.getPosition()
                    ));
                }

                if (!contextStack.isEmpty()) {
                    errors.add(new SyntaxError(
                            "Не вистачає " + contextStack.size() + " закриваючої дужки.",
                            token.getPosition()
                    ));
                }

                continue;
            }

            if (tokenType == TokenType.FUNCTION) {
                pendingFunction = token.getValue();
            }

            if (tokenType == TokenType.LEFT_PAREN) {
                contextStack.push(new Context(pendingFunction));
                pendingFunction = null;
            }

            if (tokenType == TokenType.COMMA) {
                if (contextStack.isEmpty()) {
                    errors.add(new SyntaxError(
                            "Кома не може використовуватися поза функцією.",
                            token.getPosition()
                    ));
                    previousToken = token;
                    continue;
                }

                Context context = contextStack.peek();
                if (!"pow".equals(context.getFunctionName())) {
                    errors.add(new SyntaxError(
                            "Кома дозволена тільки для розділення аргументів функції 'pow'.",
                            token.getPosition()
                    ));
                    previousToken = token;
                    continue;
                }

                if (state != State.EXPECT_OPERATOR) {
                    errors.add(new SyntaxError(
                            "Перед комою повинен бути аргумент функції 'pow'.",
                            token.getPosition()
                    ));
                    previousToken = token;
                    continue;
                }

                context.incrementArgumentCount();
                state = State.EXPECT_OPERAND;
                previousToken = token;
                continue;
            }

            if (tokenType == TokenType.RIGHT_PAREN) {
                if (contextStack.isEmpty()) {
                    errors.add(new SyntaxError(
                            "Закриваюча дужка ')' не має відповідної відкриваючої дужки.",
                            token.getPosition()
                    ));

                    previousToken = token;
                    continue;
                }

                Context context = contextStack.peek();

                if (state == State.EXPECT_OPERAND) {
                    String message;
                    if (previousToken.getType() == TokenType.LEFT_PAREN) {
                        message = "Порожні дужки '()' не допускаються.";
                    } else {
                        message = "Перед закриваючою дужкою ')' повинен бути операнд.";
                    }

                    errors.add(new SyntaxError(
                            message,
                            token.getPosition()
                    ));

                    contextStack.pop();
                    state = State.EXPECT_OPERATOR;
                    previousToken = token;
                    continue;
                }

                if ("pow".equals(context.getFunctionName())
                        && context.getArgumentCount() != 2) {

                    errors.add(new SyntaxError(
                            "Функція 'pow' повинна мати рівно 2 аргументи, " + "але отримано " + context.getArgumentCount() + ".",
                            token.getPosition()
                    ));
                }

                contextStack.pop();
                state = State.EXPECT_OPERATOR;
                previousToken = token;
                continue;
            }

            State nextState = transitions
                    .get(state)
                    .get(tokenType);

            if (nextState == null) {
                errors.add(new SyntaxError(
                        getErrorMessage(state, token, previousToken, pendingFunction),
                        token.getPosition()
                ));
                previousToken = token;
                continue;
            }

            state = nextState;
            previousToken = token;
        }

        return errors;
    }

    private void initializeTransitions() {
        Map<TokenType, State> expectOperand = new HashMap<>();
        expectOperand.put(TokenType.NUMBER, State.EXPECT_OPERATOR);
        expectOperand.put(TokenType.IDENTIFIER, State.EXPECT_OPERATOR);
        expectOperand.put(TokenType.CONSTANT, State.EXPECT_OPERATOR);
        expectOperand.put(TokenType.LEFT_PAREN, State.EXPECT_OPERAND);
        expectOperand.put(TokenType.FUNCTION, State.EXPECT_FUNCTION_PAREN);

        Map<TokenType, State> expectOperator = new HashMap<>();
        expectOperator.put(TokenType.PLUS, State.EXPECT_OPERAND);
        expectOperator.put(TokenType.MINUS, State.EXPECT_OPERAND);
        expectOperator.put(TokenType.MULTIPLY, State.EXPECT_OPERAND);
        expectOperator.put(TokenType.DIVIDE, State.EXPECT_OPERAND);
        expectOperator.put(TokenType.RIGHT_PAREN, State.EXPECT_OPERATOR);

        Map<TokenType, State> expectFunctionArgument = new HashMap<>();
        expectFunctionArgument.put(TokenType.LEFT_PAREN, State.EXPECT_OPERAND);

        transitions.put(State.EXPECT_OPERAND, expectOperand);
        transitions.put(State.EXPECT_OPERATOR, expectOperator);
        transitions.put(State.EXPECT_FUNCTION_PAREN, expectFunctionArgument);

    }

    private String getErrorMessage(State state, Token token, Token previousToken, String pendingFunction) {
        TokenType type = token.getType();

        if (state == State.EXPECT_OPERAND) {
            if (type == TokenType.PLUS
                    || type == TokenType.MINUS
                    || type == TokenType.MULTIPLY
                    || type == TokenType.DIVIDE) {

                return "Неочікуваний " + type.getDescription() + ". Після попереднього оператора повинен бути операнд.";
            }

            if (isOperand(type)) {

                return "Неочікуваний " + type.getDescription() + " '. Перед ним відсутній оператор.";
            }

            if (type == TokenType.RIGHT_PAREN) {
                return "Неочікувана закриваюча дужка ')'. " +
                        "Перед нею повинен бути операнд.";
            }

            return "Неочікуваний " + type.getDescription() + ".";
        }

        if (state == State.EXPECT_OPERATOR) {
            if (isOperand(type)) {

                return "Між " + previousToken.getType().getDescription() + " та " +
                        type.getDescription() + " відсутній оператор.";
            }

            if (type == TokenType.LEFT_PAREN) {

                return "Між " + previousToken.getType().getDescription() +
                        " та відкриваючою дужкою '(' відсутній оператор.";
            }

            return "Неочікуваний " + type.getDescription() + ". Очікувався оператор.";
        }

        if (state == State.EXPECT_FUNCTION_PAREN) {
            return "Після функції '" + pendingFunction + "' очікувалася відкриваюча дужка '('.";
        }

        return "Синтаксична помилка.";
    }

    private static class Context {
        private final String functionName;
        private int argumentCount;

        public Context(String functionName) {
            this.functionName = functionName;
            this.argumentCount = 0;
        }

        public String getFunctionName() {
            return functionName;
        }

        public int getArgumentCount() {
            return argumentCount + 1;
        }

        public void incrementArgumentCount() {
            argumentCount++;
        }
    }
}
