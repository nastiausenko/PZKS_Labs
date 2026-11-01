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
            List<SyntaxError> syntaxErrors = syntaxAnalyzer.analyze(tokens);

            if (!lexicalErrors.isEmpty() || !syntaxErrors.isEmpty()) {
                printErrors(input, lexicalErrors, syntaxErrors);
                continue;
            }

            processExpression(tokens);
        }

        scanner.close();
    }

    private static void processExpression(List<Token> tokens) {
        System.out.println("\nПомилок не виявлено");

        ExpressionNode root = new ExpressionParser(tokens).parse();
        printTree("Початкове дерево", root);

        String initialStatistics = formatTreeStatistics("До оптимізації", root);

        try {
            ExpressionNode optimizedRoot = new ExpressionOptimizer().optimize(root);
            printTree("Оптимізоване дерево", optimizedRoot);
            String optimizedStatistics = formatTreeStatistics("Після оптимізації", optimizedRoot);

            ExpressionNode parallelRoot = new ParallelTreeBuilder().build(optimizedRoot);
            printTree("Дерево паралельної форми", parallelRoot);
            printLevels(parallelRoot);
            printComparison(initialStatistics, optimizedStatistics, parallelRoot);
        } catch (ArithmeticException exception) {
            System.out.println("\nПомилка оптимізації: " + exception.getMessage());
        }
    }

    private static void printTree(String title, ExpressionNode root) {
        System.out.println("\n" + title + ":");
        root.printTree("", true);
    }

    private static void printLevels(ExpressionNode root) {
        List<List<ExpressionNode>> levels = root.getLevels();

        for (int i = 0; i < levels.size(); i++) {
            System.out.print("Ярус " + (i + 1) + ": ");

            for (ExpressionNode operation : levels.get(i)) {
                System.out.print(operation.getValue() + " ");
            }

            System.out.println();
        }
    }

    private static void printComparison(String initialStatistics, String optimizedStatistics, ExpressionNode parallelRoot) {
        System.out.println("\nПорівняння дерев:");
        System.out.printf("%-26s | %8s | %6s | %12s%n", "Етап", "Операції", "Яруси", "Макс. ширина");
        System.out.println("-".repeat(61));
        System.out.print(initialStatistics);
        System.out.print(optimizedStatistics);
        System.out.print(formatTreeStatistics("Після розпаралелювання", parallelRoot));
    }

    private static String formatTreeStatistics(String stage, ExpressionNode root) {
        return String.format("%-26s | %8d | %6d | %12d%n",
                stage, root.getOperationCount(), root.getHeight(), root.getWidth());
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
