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
            List<LexicalError> lexicalErrors = lexer.getErrors();
            List<SyntaxError> errors = syntaxAnalyzer.analyze(tokens);

            System.out.println("\nТокени:");

            for (Token token : tokens) {
                if (token.getType() != TokenType.END) {
                    System.out.printf("  %-12s '%s'%n", token.getType(), token.getValue());
                }
            }

            if (!lexicalErrors.isEmpty() || !errors.isEmpty()) {
                printErrors(input, lexicalErrors, errors);
            } else {
                System.out.println("\nПомилок не виявлено");
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
