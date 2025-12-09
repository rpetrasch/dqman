package org.dqman.streamspredicates1;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.time.LocalDate;

import org.dqman.streamspredicates2.Customer;
import org.dqman.streamspredicates2.Quotation;

/**
 * This class is used to demonstrate the use of predicates and streams (simple
 * example)
 */
public class MainPredicates {

    /**
     * This is the main method of the class.
     * 
     * @param args Command line arguments (not used)
     */
    public static void main(String[] args) {

        // --- 1. Setup Data ---
        Customer customerAlice = new Customer(1, "Alice Corp", new BigDecimal("50000.00"));
        Customer customerBob = new Customer(2, "Bob Logistics", new BigDecimal("10000.00"));

        List<Quotation> quotations = List.of(
                // Out of Range - Expired
                new Quotation(101, customerAlice, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 6, 30),
                        new BigDecimal("1500.00")),
                // In Range - High Amount, High Priority
                new Quotation(102, customerBob, LocalDate.of(2025, 1, 15), LocalDate.of(2025, 2, 15),
                        new BigDecimal("800.00")),
                // In Range - Low Amount, Low Priority
                new Quotation(103, customerAlice, LocalDate.of(2025, 3, 1), LocalDate.of(2025, 4, 1),
                        new BigDecimal("200.00")),
                // Out of Range - Future
                new Quotation(104, customerBob, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1),
                        new BigDecimal("500.00")),
                // In Range - Medium Amount, Medium Priority
                new Quotation(105, customerAlice, LocalDate.of(2025, 2, 20), LocalDate.of(2025, 3, 20),
                        new BigDecimal("450.00")));

        System.out.println("--- All Quotations ---");
        quotations.forEach(q -> System.out.println(
                q.id() + ": " + q.totalAmount() + " | Issue: " + q.issueDate() + " | Exp: " + q.expirationDate()));
        System.out.println("------------------------------------");

        LocalDate rangeStart = LocalDate.of(2025, 1, 1);
        LocalDate rangeEnd = LocalDate.of(2025, 3, 31);
        float threshold = 400.0f;

        // --- 2. Predicate Definition and Composition (Filtering Logic) ---

        // Predicate 1: Check if the Issue Date is ON or AFTER the start date
        Predicate<Quotation> isWithinDateRange = getQuotationPredicate(rangeStart, rangeEnd);

        // Add another Predicate for a numerical filter: Total Amount must be greater
        // than the threshold, e.g. 400.00
        Predicate<Quotation> isOverThreshold = q -> q.totalAmount().compareTo(new BigDecimal(threshold)) >= 0;

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
        filteredAndSorted.forEach(q -> System.out.println(
                q.id() + ": " + q.totalAmount() + " | Issue: " + q.issueDate() + " | Exp: " + q.expirationDate()));
        System.out.println("------------------END------------------");
    }

    /**
     * This factory method returns a predicate that checks if a quotation is within
     * a given date range.
     * 
     * @param rangeStart The start date of the range.
     * @param rangeEnd   The end date of the range.
     * @return A predicate that checks if a quotation is within the given date
     *         range.
     */
    private static Predicate<Quotation> getQuotationPredicate(LocalDate rangeStart, LocalDate rangeEnd) {

        // Predicate 1: Check if the Issue Date is ON or AFTER the start date
        // isAfter is exclusive, so we adjust by 1 day
        Predicate<Quotation> isIssuedAfterStart = q -> q.issueDate().isAfter(rangeStart.minusDays(1));

        // Predicate 2: Check if the Expiration Date is ON or BEFORE the end date
        // isBefore is exclusive, so we adjust by 1 day
        Predicate<Quotation> isExpiredBeforeEnd = q -> q.expirationDate().isBefore(rangeEnd.plusDays(1));

        // Predicate Composition: Combine the two date checks using logical AND
        return isIssuedAfterStart.and(isExpiredBeforeEnd);
    }

}
