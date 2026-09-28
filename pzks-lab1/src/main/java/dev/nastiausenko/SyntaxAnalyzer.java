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
                if (handleComma(token, state, contextStack, errors)) {
                    state = State.EXPECT_OPERAND;
                    previousToken = token;
                }
                continue;
            }

            if (tokenType == TokenType.RIGHT_PAREN) {
                state = handleRightParen(token, state, previousToken, contextStack, errors);
                previousToken = token;
                continue;
            }

            State nextState = getNextState(state, token, previousToken, pendingFunction, errors);

            if (nextState == null) {
                if (tokenType == TokenType.MINUS && state == State.EXPECT_OPERAND) {
                    state = State.EXPECT_NEGATIVE_OPERAND;
                    previousToken = token;
                } else if (tokenType == TokenType.LEFT_PAREN) {
                    state = State.EXPECT_OPERAND;
                    previousToken = token;
                } else if (tokenType == TokenType.FUNCTION && state == State.EXPECT_OPERATOR) {
                    state = State.EXPECT_FUNCTION_PAREN;
                    previousToken = token;
                } else if (state == State.EXPECT_OPERATOR && isOperand(tokenType)) {
                    previousToken = token;
                } else if (state == State.EXPECT_FUNCTION_PAREN) {
                    pendingFunction = null;

                    if (isOperand(tokenType)) {
                        state = State.EXPECT_OPERATOR;
                        previousToken = token;
                    } else {
                        state = State.EXPECT_OPERAND;
                        previousToken = null;
                    }
                } else if (state == State.EXPECT_NEGATIVE_OPERAND) {
                    previousToken = token;
                }

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
        if ((state == State.EXPECT_OPERAND || state == State.EXPECT_NEGATIVE_OPERAND) && previousToken != null) {
            errors.add(new SyntaxError(
                    "Вираз закінчується " + previousToken.getType().getDescription() + " '" + previousToken.getValue() + "'.",
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

    private boolean handleComma(Token token, State state, Deque<Context> contextStack, List<SyntaxError> errors) {
        if (contextStack.isEmpty()) {
            errors.add(new SyntaxError("Кома не може використовуватися поза функцією.",
                    token.getPosition()
            ));
            return false;
        }

        Context context = contextStack.peek();
        String functionName = context.getFunctionName();

        if (functionName == null) {
            errors.add(new SyntaxError("Кома не може використовуватися поза функцією.",
                    token.getPosition()
            ));
            return false;
        }

        boolean supportsMultipleArguments = "pow".equals(functionName) || functionName.matches("f\\d+");

        if (!supportsMultipleArguments) {
            errors.add(new SyntaxError("Функція '" + functionName + "' повинна мати лише один аргумент.",
                    token.getPosition()
            ));

            return false;
        }

        if (state != State.EXPECT_OPERATOR) {
            errors.add(new SyntaxError("Перед комою повинен бути аргумент функції.",
                    token.getPosition()
            ));
            return false;
        }

        if (context.getArgumentCount() >= 2) {
            errors.add(new SyntaxError(
                    "Функція '" + functionName + "' не може мати більше 2 аргументів.",
                    token.getPosition()
            ));
            context.setTooManyArguments();
            return false;
        }

        context.incrementArgumentCount();
        return true;
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
        if (context.hasTooManyArguments()) {
            contextStack.pop();
            return State.EXPECT_OPERATOR;
        }

        if (state == State.EXPECT_OPERAND || state == State.EXPECT_NEGATIVE_OPERAND) {
            String message;

            if (state == State.EXPECT_NEGATIVE_OPERAND) {
                message = "Перед закриваючою дужкою ')' після '-' повинен бути операнд.";
            } else if (previousToken != null && previousToken.getType() == TokenType.LEFT_PAREN) {
                message = "Порожні дужки '()' не допускаються.";
            } else {
                message = "Перед закриваючою дужкою ')' повинен бути операнд.";
            }

            errors.add(new SyntaxError(message, token.getPosition()));

            contextStack.pop();
            return State.EXPECT_OPERATOR;
        }

        String functionName = context.getFunctionName();
        boolean isCustomFunction = functionName != null && functionName.matches("f\\d+");

        if ("pow".equals(functionName) && context.getArgumentCount() != 2) {
            errors.add(new SyntaxError("Функція '" + functionName + "' повинна мати рівно 2 аргументи, "
                            + "але отримано " + context.getArgumentCount() + ".",
                    token.getPosition()
            ));
        }

        if (isCustomFunction && (context.getArgumentCount() < 1 || context.getArgumentCount() > 2)) {
            errors.add(new SyntaxError(
                    "Функція '" + functionName + "' повинна мати від 1 до 2 аргументів.",
                    token.getPosition()
            ));
        }

        contextStack.pop();
        return State.EXPECT_OPERATOR;
    }

    private State getNextState(State state, Token token, Token previousToken, String pendingFunction, List<SyntaxError> errors) {
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
            errors.add(new SyntaxError(getErrorMessage(state, token, previousToken, pendingFunction), token.getPosition()));
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

                if (previousToken == null) {
                    return "Вираз не може починатися з оператора '" + token.getValue() + "'.";
                }

                return "Неочікуваний " + type.getDescription() + " '" + token.getValue() + "'"
                        + ". Після попереднього оператора повинен бути операнд.";
            }

            if (isOperand(type)) {
                return "Неочікуваний " + type.getDescription() + " '" + token.getValue() + "'"
                        + " '. Перед ним відсутній оператор.";
            }

            if (type == TokenType.RIGHT_PAREN) {
                return "Неочікувана закриваюча дужка ')'. " +
                        "Перед нею повинен бути операнд.";
            }

            return "Неочікуваний " + type.getDescription() + " '" + token.getValue() + "'.";
        }

        if (state == State.EXPECT_OPERATOR) {
            if (isOperand(type)) {
                return "Між " + previousToken.getType().getDescription() + " '" + previousToken.getValue() + "' та " +
                        type.getDescription() + " '" + token.getValue() + "' відсутній оператор.";
            }

            if (type == TokenType.LEFT_PAREN) {
                return "Між " + previousToken.getType().getDescription() + " '" + previousToken.getValue() +
                        "' та відкриваючою дужкою '(' відсутній оператор.";
            }

            return "Неочікуваний " + type.getDescription() + " '" + token.getValue() + "'. Очікувався оператор.";
        }

        if (state == State.EXPECT_NEGATIVE_OPERAND) {
            return "Після оператора '-' повинен бути операнд.";
        }

        if (state == State.EXPECT_FUNCTION_PAREN) {
            return "Після функції '" + pendingFunction + "' очікувалася відкриваюча дужка '('.";
        }

        return "Синтаксична помилка.";
    }

    private static class Context {
        private final String functionName;
        private int argumentCount;
        private boolean tooManyArguments;

        public Context(String functionName) {
            this.functionName = functionName;
            this.argumentCount = 0;
            this.tooManyArguments = false;
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

        public boolean hasTooManyArguments() {
            return tooManyArguments;
        }

        public void setTooManyArguments() {
            tooManyArguments = true;
        }
    }
}
