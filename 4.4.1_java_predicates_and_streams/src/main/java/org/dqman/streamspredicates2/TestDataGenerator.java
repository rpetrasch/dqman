package org.dqman.streamspredicates2;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Generates sample data for testing data quality rules.
 */
public class TestDataGenerator {

    private static final Random RAND = new Random(); // Random number generator
    private static final List<String> FIRST_NAMES = List.of("John", "Jon", "Jane", "Jayne", "Peter", "Pete", "Sarah", "Sara");
    private static final List<String> LAST_NAMES = List.of("Smith", "Jones", "Williams", "Brown", "Davis");

    /**
     * Generates a list of sample quotations.
     * @param count Number of quotations to generate
     * @return List of quotations
     */
    public static List<Quotation> generateQuotations(int count) {
        System.out.printf("Generating %d sample quotations...%n", count);
        List<Customer> customers = generateCustomers(count / 10); // Create a pool of customers
        List<Quotation> quotations = new ArrayList<>();
        int errorType1Counter = 0;
        int errorType2Counter = 0;

        for (int i = 0; i < count; i++) {
            Customer customer = customers.get(RAND.nextInt(customers.size()));
            LocalDate issueDate = LocalDate.now().minusDays(RAND.nextInt(365));
            BigDecimal totalAmount = BigDecimal.valueOf(RAND.nextDouble(customer.maxCreditLine().doubleValue()));
            LocalDate expirationDate = issueDate.plusDays(RAND.nextInt(90) + 1);
            
            // Intentionally inject DQ errors into ~10% of the data
            int errorType = RAND.nextInt(20);
            if (errorType == 1) { // Error: Invalid expiration date
                expirationDate = issueDate.minusDays(RAND.nextInt(10) + 1);
                errorType1Counter++;
            } else if (errorType == 2) { // Error: Exceeds credit line
                totalAmount = customer.maxCreditLine().add(BigDecimal.valueOf(RAND.nextDouble(1000)));
                errorType2Counter++;
            }
            
            quotations.add(new Quotation(1000L + i, customer, issueDate, expirationDate, totalAmount));
        }
        System.out.println("Data generation complete: " + quotations.size() + " quotations generated.");
        System.out.println("Error Type 1 (Invalid expiration date) injected: " + errorType1Counter);
        System.out.println("Error Type 2 (Exceeds credit line) injected: " + errorType2Counter);
        System.out.println("Total errors injected: "+ (errorType1Counter + errorType2Counter) +". Percentage of injected errors: " + ((errorType1Counter + errorType2Counter) / (double) count) * 100 + "%");
        return quotations;
    }

    /**
     * Generates a list of sample customers.
     * @param count Number of customers to generate
     * @return List of customers
     */
    private static List<Customer> generateCustomers(int count) {
        List<Customer> customers = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String name = FIRST_NAMES.get(RAND.nextInt(FIRST_NAMES.size())) + " " +
                          LAST_NAMES.get(RAND.nextInt(LAST_NAMES.size()));
            BigDecimal creditLine = BigDecimal.valueOf(RAND.nextInt(Customer.MAX_CREDIT_LINE_INT));
            customers.add(new Customer(200L + i, name, creditLine));
        }
        return customers;
    }
}