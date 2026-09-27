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

            List<Token> tokens = lexer.tokenize(input);
            List<SyntaxError> errors = syntaxAnalyzer.analyze(tokens);

            for (Token token : tokens) {
                System.out.println(token);
            }

            if (!errors.isEmpty()) {
                printErrors(input, errors);
            } else {
                System.out.println("Помилок не виявлено");
            }
        }

        scanner.close();
    }

    private static void printErrors(String expression, List<SyntaxError> errors) {
        System.out.println("\nПомилки:");
        System.out.println(expression);

        for (int i = 0; i <= expression.length(); i++) {
            if (hasErrorAtPosition(errors, i)) {
                System.out.print("^");
            } else {
                System.out.print(" ");
            }
        }

        System.out.println();

        for (SyntaxError error : errors) {
            System.out.println(error);
        }
    }
    private static boolean hasErrorAtPosition(List<SyntaxError> errors, int position) {
        for (SyntaxError error : errors) {
            if (error.getPosition() == position) {
                return true;
            }
        }

        return false;
    }
}
