package org.dqman;

/**
 * Demonstrates common pitfalls with floating-point arithmetic in Java,
 * including precision errors and catastrophic cancellation.
 * Tested woth Java 25.
 */
public class Main {

    /**
     * Case 1: Repeated addition
     * Demonstrates precision errors caused by repeated addition.
     */
    public static void escalationAddition() {
        double result = 0.0;
        for (int i = 0; i < 1000000; i++) {
            result += 0.1;
        }

        double expected = 100000.0;
        double error = Math.abs(expected - result);

        System.out.printf("Expected: %.15f%n", expected);
        System.out.printf("Actual:   %.15f%n", result);
        System.out.printf("Error:    %.15f%n", error);
        System.out.printf("Error %%:  %.10f%%%n%n", (error / expected) * 100);
    }

    /**
     * Case 2: Summation with different magnitudes
     * Demonstrates catastrophic cancellation.
     */
    public static void sumDifferentMagnitudes() {
        double large = 1e16;
        double sum = large;

        // Add small values to large number
        for (int i = 0; i < 1000; i++) {
            sum += 1.0;
        }
        sum -= large;

        double expected = 1000.0;
        System.out.println("Adding small values to large number:");
        System.out.printf("Expected: %.15f%n", expected);
        System.out.printf("Actual:   %.15f%n", sum);
        System.out.printf("Error:    %.15f%n%n", Math.abs(expected - sum));
    }

    /**
     * Case 3: Non-associativity of floating point
     * Demonstrates non-associativity of floating-point arithmetic.
     */
    public static void nonAssociativity() {
        double a = 0.1;
        double b = 0.2;
        double c = 0.3;

        // Different grouping orders
        double result1 = (a + b) + c;
        double result2 = a + (b + c);

        System.out.println("Non-associativity: (0.1 + 0.2) + 0.3 vs 0.1 + (0.2 + 0.3)");
        System.out.printf("(a + b) + c = %.20f%n", result1);
        System.out.printf("a + (b + c) = %.20f%n", result2);
        System.out.printf("Difference:   %.20e%n%n", Math.abs(result1 - result2));
    }

    /**
     * Case 4: Iterative computation with varying inputs
     * Demonstrates order-dependent summation errors.
     */
    public static void iterativeComputation() {
        double[] values = new double[1000];
        java.util.Random rand = new java.util.Random(42);

        // Generate random values
        for (int i = 0; i < values.length; i++) {
            values[i] = rand.nextDouble() * 0.01;
        }

        // Sum forward
        double sumForward = 0.0;
        for (int i = 0; i < values.length; i++) {
            sumForward += values[i];
        }

        // Sum backward
        double sumBackward = 0.0;
        for (int i = values.length - 1; i >= 0; i--) {
            sumBackward += values[i];
        }

        System.out.println("Order-dependent summation:");
        System.out.printf("Forward sum:  %.15f%n", sumForward);
        System.out.printf("Backward sum: %.15f%n", sumBackward);
        System.out.printf("Difference:   %.15e%n%n", Math.abs(sumForward - sumBackward));
    }

    public static void main(String[] args) {
        System.out.println("-- Example: Java floating point precision error escalation ---\n");
        escalationAddition();
        sumDifferentMagnitudes();
        nonAssociativity();
        iterativeComputation();
    }
}
