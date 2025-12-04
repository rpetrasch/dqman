package org.dqman.streamspredicates2;

import java.util.*;

import static org.dqman.streamspredicates2.FindDuplicates.findDuplicateCustomerQuotesBucketingParallelStream;
import static org.dqman.streamspredicates2.FindDuplicates.findDuplicateCustomerQuotesBucketingStream;

/**
 * Main class for running the data quality checks with both sequential and parallel streams.
 * First, it generates a list of sample quotations. Then, it runs the data quality checks on both streams.
 */
public class MainStreamDataQualityProcessor {

    private static final int DATA_SIZE = 5000;  // number of generated quotations
    private static final double SIMILARITY_THRESHOLD = 0.90;  // Minimum value for similarity threshold

    // A record to hold the result of validating a single quotation
    record ValidationResult(Quotation quotation, List<String> errors) {}

    /** Main method to run the data quality checks. */
    public static void main(String[] args) {

        // 1. TEST DATA GENERATION ---
        System.out.println("--- Generating test data ---");
        List<Quotation> quotations = TestDataGenerator.generateQuotations(DATA_SIZE);  // Generate sample data

        // 2. SEQUENTIAL STREAM PROCESSING ---
        System.out.println("\n--- Running DQ checks with SEQUENTIAL stream ---");
        long startTimeSequential = System.nanoTime();
        // Step 1: Apply predicate-based DQ rules
        List<ValidationResult> sequentialErrors = quotations.stream()
                .map(MainStreamDataQualityProcessor::validateQuotation)
                .filter(result -> !result.errors().isEmpty())
                .toList();
        // Step 2: Apply context-aware duplicate check
        Set<Quotation> sequentialDuplicates = findDuplicateCustomerQuotesBucketingStream(quotations, SIMILARITY_THRESHOLD);
        long endTimeSequential = System.nanoTime();
        double durationSequential = (endTimeSequential - startTimeSequential) / 1_000_000.0;

        System.out.printf("Found %d quotations with predicate-based errors.%n", sequentialErrors.size());
        System.out.printf("Found %d quotations from potentially duplicate customers.%n", sequentialDuplicates.size());
        System.out.printf("Sequential processing took: %.2f ms%n", durationSequential);

        // 3. PARALLEL STREAM PROCESSING ---
        System.out.println("\n--- Running DQ checks with PARALLEL stream ---");
        long startTimeParallel = System.nanoTime();
        // Step 1: Apply predicate-based rules in parallel
        List<ValidationResult> parallelErrors = quotations.parallelStream()
                .map(MainStreamDataQualityProcessor::validateQuotation)
                .filter(result -> !result.errors().isEmpty())
                .toList();
        // Step 2: The duplicate check is not easily parallelized with streams, but we run it again for fair timing.
        Set<Quotation> parallelDuplicates = findDuplicateCustomerQuotesBucketingParallelStream(quotations, SIMILARITY_THRESHOLD);
        long endTimeParallel = System.nanoTime();
        double durationParallel = (endTimeParallel - startTimeParallel) / 1_000_000.0;

        System.out.printf("Found %d quotations with predicate-based errors.%n", parallelErrors.size());
        System.out.printf("Found %d quotations from potentially duplicate customers.%n", parallelDuplicates.size());
        System.out.printf("Parallel processing took: %.2f ms%n", durationParallel);

        // 4. LAPSED TIME DIFFERENCE ---
        System.out.println("\n--- Performance Comparison ---");
        System.out.printf("Sequential Time: %.2f ms%n", durationSequential);
        System.out.printf("Parallel Time:   %.2f ms%n", durationParallel);
        if (durationParallel > 0) {
            double speedup = durationSequential / durationParallel;
            System.out.printf("Speedup: %.2fx%n", speedup);
        }
    }

    /**
     * Applies all predicates to a single quotation and returns a result object.
     */
    private static ValidationResult validateQuotation(Quotation q) {
        List<String> errors = new ArrayList<>();
        if (!QuotationDataQualityRules.IS_EXPIRATION_DATE_VALID.test(q)) {
            errors.add("Expiration date is not after issue date.");
        }
        if (!QuotationDataQualityRules.IS_WITHIN_CREDIT_LINE.test(q)) {
            errors.add("Total amount exceeds customer's credit line.");
        }
        return new ValidationResult(q, errors);
    }

}