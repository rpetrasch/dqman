package org.dqman;

public class Main {

    public static void main(String[] args) {

        int number = 10; // number is fixed as an integer.	Static: Type checked at compile time.
        String text = "5";	// text is fixed as a string.
        // int result = number + text; // Compile-Time Error: Java is strongly and statically typed.

        int my_int = 10; // fixed as an integer.
        // my_int = "hello";  // Type error
    }
}