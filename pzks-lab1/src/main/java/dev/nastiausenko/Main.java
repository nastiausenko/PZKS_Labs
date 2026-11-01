package dev.nastiausenko;

import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        Lexer lexer = new Lexer();
        SyntaxAnalyzer syntaxAnalyzer = new SyntaxAnalyzer();

        while (true) {
            System.out.print("> ");
            if (!scanner.hasNextLine()) {
                break;
            }

            String input = scanner.nextLine();
            if (input.equals("exit")) {
                break;
            }

            if (input.isBlank()) {
                continue;
            }

            List<Token> tokens = lexer.tokenize(input);
            List<LexicalError> lexicalErrors = lexer.getErrors();
            List<SyntaxError> errors = syntaxAnalyzer.analyze(tokens);

            if (!lexicalErrors.isEmpty() || !errors.isEmpty()) {
                printErrors(input, lexicalErrors, errors);
            } else {
                System.out.println("\nПомилок не виявлено");

                ExpressionParser expressionParser = new ExpressionParser(tokens);
                ExpressionNode root = expressionParser.parse();

                System.out.println("\nПочаткове дерево:");
                root.printTree("", true);

                try {
                    ExpressionOptimizer optimizer = new ExpressionOptimizer();
                    ExpressionNode optimizedRoot = optimizer.optimize(root);

                    System.out.println("\nОптимізоване дерево:");
                    optimizedRoot.printTree("", true);

                    ParallelTreeBuilder treeBuilder = new ParallelTreeBuilder();
                    ExpressionNode parallelRoot = treeBuilder.build(optimizedRoot);

                    System.out.println("\nДерево паралельної форми:");
                    parallelRoot.printTree("", true);
                    System.out.println("Кількість ярусів операцій: " + parallelRoot.getHeight());
                    System.out.println("Максимальна ширина: " + parallelRoot.getWidth());

                    List<List<ExpressionNode>> levels = parallelRoot.getLevels();

                    for (int i = 0; i < levels.size(); i++) {
                        System.out.print("Ярус " + (i + 1) + ": ");

                        for (ExpressionNode operation : levels.get(i)) {
                            System.out.print(operation.getValue() + " ");
                        }

                        System.out.println();
                    }

                } catch (ArithmeticException exception) {
                    System.out.println("\nПомилка оптимізації: " + exception.getMessage());
                }
            }
        }

        scanner.close();
    }

    private static void printErrors(String expression, List<LexicalError> lexicalErrors, List<SyntaxError> syntaxErrors) {
        System.out.println("\nПомилки:");
        System.out.println(expression);

        for (int i = 0; i <= expression.length(); i++) {
            if (hasLexicalErrorAtPosition(lexicalErrors, i) || hasSyntaxErrorAtPosition(syntaxErrors, i)) {
                System.out.print("^");
            } else {
                System.out.print(" ");
            }
        }

        System.out.println();

        int errorNumber = 1;

        for (LexicalError error : lexicalErrors) {
            System.out.println(errorNumber++ + ". Лексична помилка, позиція " + error.position()
                    + ": " + error.error());
        }

        for (SyntaxError error : syntaxErrors) {
            System.out.println(errorNumber++ + ". Синтаксична помилка, позиція " + error.getPosition()
                    + ": " + error.getError());
        }
    }

    private static boolean hasLexicalErrorAtPosition(List<LexicalError> errors, int position) {
        for (LexicalError error : errors) {
            if (error.position() == position) {
                return true;
            }
        }

        return false;
    }

    private static boolean hasSyntaxErrorAtPosition(List<SyntaxError> errors, int position) {
        for (SyntaxError error : errors) {
            if (error.getPosition() == position) {
                return true;
            }
        }

        return false;
    }
}
