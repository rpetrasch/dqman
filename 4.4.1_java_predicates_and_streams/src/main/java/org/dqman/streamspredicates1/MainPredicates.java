package org.dqman.streamspredicates1;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.time.LocalDate;
import java.util.stream.Collectors;

import org.dqman.streamspredicates2.Customer;
import org.dqman.streamspredicates2.Quotation;
import org.dqman.streamspredicates2.TestDataGenerator;


public class MainPredicates {

    private static boolean test = true;

    public static void main(String[] args) {
        Predicate<Integer> isEven = n -> n % 2 == 0;

        boolean result1 = isEven.test(4);  // returns true
        boolean result2 = isEven.test(7);  // returns false

        System.out.println("The test result for the number 7 is: " + result2);

        List<Quotation> quotations = null;

        if (test) {
            // --- 1. Setup Data ---
            Customer c1 = new Customer(1, "Alice Corp", new BigDecimal("50000.00"));
            Customer c2 = new Customer(2, "Bob Logistics", new BigDecimal("10000.00"));

            quotations = List.of(
                    // Out of Range - Expired
                    new Quotation(101, c1, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 6, 30), new BigDecimal("1500.00")),
                    // In Range - High Amount, High Priority
                    new Quotation(102, c2, LocalDate.of(2025, 1, 15), LocalDate.of(2025, 2, 15), new BigDecimal("800.00")),
                    // In Range - Low Amount, Low Priority
                    new Quotation(103, c1, LocalDate.of(2025, 3, 1), LocalDate.of(2025, 4, 1), new BigDecimal("200.00")),
                    // Out of Range - Future
                    new Quotation(104, c2, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1), new BigDecimal("500.00")),
                    // In Range - Medium Amount, Medium Priority
                    new Quotation(105, c1, LocalDate.of(2025, 2, 20), LocalDate.of(2025, 3, 20), new BigDecimal("450.00"))
            );
        } else {
            quotations = TestDataGenerator.generateQuotations(5000);
            System.out.println(quotations.get(1));


            // Define the target date range for filtering

        }

        System.out.println("--- All Quotations ---");
        quotations.forEach(q -> System.out.println(q.id() + ": " + q.totalAmount() + " | Issue: " + q.issueDate() + " | Exp: " + q.expirationDate()));
        System.out.println("------------------------------------");

        LocalDate rangeStart = LocalDate.of(2025, 1, 1);
        LocalDate rangeEnd = LocalDate.of(2025, 3, 31);


        // --- 2. Predicate Definition and Composition (Filtering Logic) ---

        // Predicate 1: Check if the Issue Date is ON or AFTER the start date
        Predicate<Quotation> isWithinDateRange = getQuotationPredicate(rangeStart, rangeEnd);

        // Add another Predicate for a numerical filter: Total Amount must be greater than $400
        Predicate<Quotation> isOverThreshold = q -> q.totalAmount().compareTo(new BigDecimal("400.00")) >= 0;

        // Final Composite Predicate: Must be within date range AND over the threshold
        Predicate<Quotation> finalFilter = isWithinDateRange.and(isOverThreshold);


        // --- 3. Comparator Definition (Sorting Logic) ---

        // Sort 1: Primary sort by Total Amount (Highest to Lowest)
        Comparator<Quotation> sortByAmount = Comparator.comparing(Quotation::totalAmount).reversed();

        // Sort 2: Secondary sort by Issue Date (Oldest to Newest)
        Comparator<Quotation> thenSortByIssueDate = sortByAmount.thenComparing(Quotation::issueDate);

        // --- 4. Stream Pipeline Execution ---

        List<Quotation> filteredAndSorted = quotations.stream()

                // Intermediate Operation 1 (Filtering using the composite Predicate):
                .filter(finalFilter)

                // Intermediate Operation 2 (Sorting using the composite Comparator):
                .sorted(thenSortByIssueDate)

                // Terminal Operation:
                .toList();

        System.out.println("--- Filtered Quotations ---");
        filteredAndSorted.forEach(q -> System.out.println(q.id() + ": " + q.totalAmount() + " | Issue: " + q.issueDate() + " | Exp: " + q.expirationDate()));
        System.out.println("------------------END------------------");
    }

    private static Predicate<Quotation> getQuotationPredicate(LocalDate rangeStart, LocalDate rangeEnd) {

        Predicate<Quotation> isIssuedAfterStart = q -> q.issueDate().isAfter(rangeStart.minusDays(1)); // isAfter is exclusive, so we adjust by 1 day
        // Predicate 2: Check if the Expiration Date is ON or BEFORE the end date
        Predicate<Quotation> isExpiredBeforeEnd = q -> q.expirationDate().isBefore(rangeEnd.plusDays(1)); // isBefore is exclusive, so we adjust by 1 day

        // Predicate Composition: Combine the two date checks using logical AND
        return isIssuedAfterStart.and(isExpiredBeforeEnd);
    }

}
