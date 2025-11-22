package org.dqman;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TestDataGenerator {

    private static final Random RAND = new Random();
    private static final List<String> FIRST_NAMES = List.of("John", "Jon", "Jane", "Jayne", "Peter", "Pete", "Sarah", "Sara");
    private static final List<String> LAST_NAMES = List.of("Smith", "Jones", "Williams", "Brown", "Davis");

    public static List<Quotation> generateQuotations(int count) {
        System.out.printf("Generating %d sample quotations...%n", count);
        List<Customer> customers = generateCustomers(count / 10); // Create a pool of customers
        List<Quotation> quotations = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            Customer customer = customers.get(RAND.nextInt(customers.size()));
            LocalDate issueDate = LocalDate.now().minusDays(RAND.nextInt(365));
            BigDecimal totalAmount = BigDecimal.valueOf(RAND.nextDouble(10000));
            LocalDate expirationDate = issueDate.plusDays(RAND.nextInt(90));
            
            // Intentionally inject DQ errors into ~20% of the data
            int errorType = RAND.nextInt(10);
            if (errorType == 1) { // Error: Invalid expiration date
                expirationDate = issueDate.minusDays(RAND.nextInt(10) + 1);
            } else if (errorType == 2) { // Error: Exceeds credit line
                totalAmount = customer.maxCreditLine().add(BigDecimal.valueOf(RAND.nextDouble(5000)));
            }
            
            quotations.add(new Quotation(1000L + i, customer, issueDate, expirationDate, totalAmount));
        }
        System.out.println("Data generation complete.");
        return quotations;
    }

    private static List<Customer> generateCustomers(int count) {
        List<Customer> customers = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String name = FIRST_NAMES.get(RAND.nextInt(FIRST_NAMES.size())) + " " +
                          LAST_NAMES.get(RAND.nextInt(LAST_NAMES.size()));
            BigDecimal creditLine = BigDecimal.valueOf(RAND.nextInt(15000) + 5000);
            customers.add(new Customer(200L + i, name, creditLine));
        }
        return customers;
    }
}