package dev.nastiausenko;

import java.util.*;

public class SyntaxAnalyzer {
    private enum State {
        EXPECT_OPERAND,
        EXPECT_NEGATIVE_OPERAND,
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
                handleEnd(token, state, previousToken, pendingFunction, contextStack, errors);
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
                state = handleComma(token, state, contextStack, errors);
                previousToken = token;
                continue;
            }

            if (tokenType == TokenType.RIGHT_PAREN) {
                state = handleRightParen(token, state, previousToken, contextStack, errors);
                previousToken = token;
                continue;
            }

            State nextState = getNextState(state, token, previousToken, errors);

            if (nextState == null) {
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

        Map<TokenType, State> expectNegativeOperand = new HashMap<>();
        expectNegativeOperand.put(TokenType.NUMBER, State.EXPECT_OPERATOR);
        expectNegativeOperand.put(TokenType.IDENTIFIER, State.EXPECT_OPERATOR);
        expectNegativeOperand.put(TokenType.CONSTANT, State.EXPECT_OPERATOR);
        expectNegativeOperand.put(TokenType.FUNCTION, State.EXPECT_FUNCTION_PAREN);
        expectNegativeOperand.put(TokenType.LEFT_PAREN, State.EXPECT_OPERAND);

        Map<TokenType, State> expectOperator = new HashMap<>();
        expectOperator.put(TokenType.PLUS, State.EXPECT_OPERAND);
        expectOperator.put(TokenType.MINUS, State.EXPECT_OPERAND);
        expectOperator.put(TokenType.MULTIPLY, State.EXPECT_OPERAND);
        expectOperator.put(TokenType.DIVIDE, State.EXPECT_OPERAND);
        expectOperator.put(TokenType.RIGHT_PAREN, State.EXPECT_OPERATOR);

        Map<TokenType, State> expectFunctionArgument = new HashMap<>();
        expectFunctionArgument.put(TokenType.LEFT_PAREN, State.EXPECT_OPERAND);

        transitions.put(State.EXPECT_OPERAND, expectOperand);
        transitions.put(State.EXPECT_NEGATIVE_OPERAND, expectNegativeOperand);
        transitions.put(State.EXPECT_OPERATOR, expectOperator);
        transitions.put(State.EXPECT_FUNCTION_PAREN, expectFunctionArgument);
    }

    private void handleEnd(Token token, State state, Token previousToken, String pendingFunction,
                           Deque<Context> contextStack, List<SyntaxError> errors) {
        if (state == State.EXPECT_OPERAND && previousToken != null) {
            errors.add(new SyntaxError(
                    "Вираз закінчується " + previousToken.getType().getDescription() + ". Після нього повинен бути операнд.",
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
    }

    private State handleComma(Token token, State state, Deque<Context> contextStack, List<SyntaxError> errors) {
        if (contextStack.isEmpty()) {
            errors.add(new SyntaxError(
                    "Кома не може використовуватися поза функцією.",
                    token.getPosition()
            ));

            return state;
        }

        Context context = contextStack.peek();
        if (!"pow".equals(context.getFunctionName())) {
            errors.add(new SyntaxError(
                    "Кома дозволена тільки для розділення аргументів функції 'pow'.",
                    token.getPosition()
            ));

            return state;
        }


        if (state != State.EXPECT_OPERATOR) {
            errors.add(new SyntaxError(
                    "Перед комою повинен бути аргумент функції 'pow'.",
                    token.getPosition()
            ));
            return state;
        }

        context.incrementArgumentCount();
        return State.EXPECT_OPERAND;
    }

    private State handleRightParen(Token token, State state, Token previousToken, Deque<Context> contextStack, List<SyntaxError> errors) {
        if (contextStack.isEmpty()) {
            errors.add(new SyntaxError(
                    "Закриваюча дужка ')' не має відповідної відкриваючої дужки.",
                    token.getPosition()
            ));
            return state;
        }

        Context context = contextStack.peek();
        if (state == State.EXPECT_OPERAND) {
            String message;
            if (previousToken.getType() == TokenType.LEFT_PAREN) {
                message = "Порожні дужки '()' не допускаються.";
            } else {
                message = "Перед закриваючою дужкою ')' повинен бути операнд.";
            }

            errors.add(new SyntaxError(message, token.getPosition()));

            contextStack.pop();
            return State.EXPECT_OPERATOR;
        }

        if ("pow".equals(context.getFunctionName()) && context.getArgumentCount() != 2) {
            errors.add(new SyntaxError(
                    "Функція 'pow' повинна мати рівно 2 аргументи, " + "але отримано " + context.getArgumentCount() + ".",
                    token.getPosition()
            ));
        }

        contextStack.pop();
        return State.EXPECT_OPERATOR;
    }

    private State getNextState(State state, Token token, Token previousToken, List<SyntaxError> errors) {
        TokenType tokenType = token.getType();

        if (tokenType == TokenType.MINUS && state == State.EXPECT_OPERAND) {
            if (previousToken == null || previousToken.getType() == TokenType.LEFT_PAREN) {
                return State.EXPECT_NEGATIVE_OPERAND;
            }

            errors.add(new SyntaxError("Від'ємний операнд у середині виразу повинен бути взятий у дужки.",
                    token.getPosition()
            ));

            return null;
        }

        State nextState = transitions
                .get(state)
                .get(tokenType);

        if (nextState == null) {
            errors.add(new SyntaxError(getErrorMessage(state, token, previousToken, null), token.getPosition()));
            return null;
        }

        return nextState;
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
