package dev.nastiausenko;

import java.util.*;

public class SyntaxAnalyzer {
    private enum State {
        EXPECT_OPERAND,
        EXPECT_OPERATOR,
        EXPECT_FUNCTION_PAREN
    }

    private final Map<State, Map<TokenType, State>> transitions = new HashMap<>();

    public SyntaxAnalyzer() {
        initializeTransitions();
    }

    public List<SyntaxError> analyze(List<Token> tokens) {
        List<SyntaxError> errors = new ArrayList<>();

        State state = State.EXPECT_OPERAND;
        Deque<Context> contextStack = new ArrayDeque<>();
        String pendingFunction = null;

        for (Token token : tokens) {
            TokenType tokenType = token.getType();

            if (tokenType == TokenType.END) {
                if (state == State.EXPECT_OPERAND) {
                    errors.add(new SyntaxError(
                            "Вираз не може закінчуватись оператором",
                            token.getPosition()
                    ));
                }

                if (!contextStack.isEmpty()) {
                    errors.add(new SyntaxError(
                            "Не вистачає закриваючої дужки",
                            token.getPosition()
                    ));
                }

                continue;
            }

            // Запам'ятовуємо функцію до відкриваючої дужки
            if (tokenType == TokenType.FUNCTION) {
                pendingFunction = token.getValue();
            }

            // Відкриваюча дужка
            if (tokenType == TokenType.LEFT_PAREN) {
                contextStack.push(new Context(pendingFunction));
                pendingFunction = null;
            }

            // Кома
            if (tokenType == TokenType.COMMA) {
                if (contextStack.isEmpty()
                        || !"pow".equals(contextStack.peek().getFunctionName())
                        || state != State.EXPECT_OPERATOR) {

                    errors.add(new SyntaxError(
                            "Кома дозволена тільки між аргументами pow",
                            token.getPosition()
                    ));
                    continue;
                }

                contextStack.peek().incrementArgumentCount();
                state = State.EXPECT_OPERAND;
                continue;
            }

            // Закриваюча дужка
            if (tokenType == TokenType.RIGHT_PAREN) {
                if (contextStack.isEmpty()) {
                    errors.add(new SyntaxError(
                            "Зайва закриваюча дужка",
                            token.getPosition()
                    ));
                    continue;
                }

                Context context = contextStack.peek();

                if (state == State.EXPECT_OPERAND) {
                    errors.add(new SyntaxError(
                            "Очікувався операнд перед ')'",
                            token.getPosition()
                    ));
                    continue;
                }

                if ("pow".equals(context.getFunctionName())
                        && context.getArgumentCount() != 2) {

                    errors.add(new SyntaxError(
                            "Функція pow повинна мати 2 аргументи",
                            token.getPosition()
                    ));
                }

                contextStack.pop();
            }

            State nextState = transitions.get(state).get(tokenType);

            if (nextState == null) {
                errors.add(new SyntaxError(
                        getErrorMessage(state, tokenType),
                        token.getPosition()
                ));
                continue;
            }

            state = nextState;
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

    private String getErrorMessage(State state, TokenType tokenType) {
        if(state == State.EXPECT_OPERAND) {
            return "Очікувався операнд";
        }

        if(state == State.EXPECT_OPERATOR) {
            return "Очікувався оператор";
        }
        return "Очікувалася відкриваюча дужка після функції";
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
