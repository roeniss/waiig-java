package org.example;

import org.example.repl.Repl;

import java.io.IOException;

public class Main {
    static void main() throws IOException {
        String user = System.getProperty("user.name");
        IO.println("Hello " + user + "! This is the Monkey programming language!");
        IO.println("Feel free to type in commands");

        Repl.start(System.in, System.out);
    }
}
