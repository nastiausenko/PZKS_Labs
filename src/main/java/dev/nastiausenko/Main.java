package dev.nastiausenko;

import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        String input = scanner.nextLine();

        Lexer lexer = new Lexer();
        List<Token> tokens = lexer.tokenize(input);

        SyntaxAnalyzer syntaxAnalyzer = new SyntaxAnalyzer();
        List<SyntaxError> errors = syntaxAnalyzer.analyze(tokens);

        for (Token token : tokens) {
            System.out.println(token);
        }

        if (!errors.isEmpty()) {
            System.out.println("\nПомилки:");
            for (SyntaxError error : errors) {
                System.out.println(error);
            }
        } else {
            System.out.println("Помилок не виявлено");
        }

        scanner.close();
    }
}
