package dev.nastiausenko;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        String input = scanner.nextLine();

        Lexer lexer = new Lexer();
        List<Token> tokens = lexer.tokenize(input);

        for (Token token : tokens) {
            System.out.println(token);
        }

        scanner.close();
    }
}
