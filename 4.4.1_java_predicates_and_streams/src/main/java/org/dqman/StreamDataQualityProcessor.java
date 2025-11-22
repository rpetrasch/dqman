package org.dqman;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class StreamDataQualityProcessor {

    private static final int DATA_SIZE = 5000;
    private static final double SIMILARITY_THRESHOLD = 0.90;

    // A record to hold the result of validating a single quotation
    record ValidationResult(Quotation quotation, List<String> errors) {}

    public static void main(String[] args) {
        List<Quotation> quotations = TestDataGenerator.generateQuotations(DATA_SIZE);

        // --- b) SEQUENTIAL STREAM PROCESSING ---
        System.out.println("\n--- Running DQ checks with SEQUENTIAL stream ---");
        long startTimeSequential = System.nanoTime();

        // Step 1: Apply predicate-based rules
        List<ValidationResult> sequentialErrors = quotations.stream()
                .map(StreamDataQualityProcessor::validateQuotation)
                .filter(result -> !result.errors().isEmpty())
                .collect(Collectors.toList());

        // Step 2: Apply context-aware duplicate check
        Set<Quotation> sequentialDuplicates = findDuplicateCustomerQuotes(quotations, SIMILARITY_THRESHOLD);

        long endTimeSequential = System.nanoTime();
        double durationSequential = (endTimeSequential - startTimeSequential) / 1_000_000.0;
        
        System.out.printf("Found %d quotations with predicate-based errors.%n", sequentialErrors.size());
        System.out.printf("Found %d quotations from potentially duplicate customers.%n", sequentialDuplicates.size());
        System.out.printf("Sequential processing took: %.2f ms%n", durationSequential);


        // --- c) PARALLEL STREAM PROCESSING ---
        System.out.println("\n--- Running DQ checks with PARALLEL stream ---");
        long startTimeParallel = System.nanoTime();

        // Step 1: Apply predicate-based rules in parallel
        List<ValidationResult> parallelErrors = quotations.parallelStream()
                .map(StreamDataQualityProcessor::validateQuotation)
                .filter(result -> !result.errors().isEmpty())
                .collect(Collectors.toList());

        // Step 2: The duplicate check is not easily parallelized with streams, but we run it again for fair timing.
        Set<Quotation> parallelDuplicates = findDuplicateCustomerQuotes(quotations, SIMILARITY_THRESHOLD);
        
        long endTimeParallel = System.nanoTime();
        double durationParallel = (endTimeParallel - startTimeParallel) / 1_000_000.0;

        System.out.printf("Found %d quotations with predicate-based errors.%n", parallelErrors.size());
        System.out.printf("Found %d quotations from potentially duplicate customers.%n", parallelDuplicates.size());
        System.out.printf("Parallel processing took: %.2f ms%n", durationParallel);
        
        
        // --- d) LAPSED TIME DIFFERENCE ---
        System.out.println("\n--- Performance Comparison ---");
        System.out.printf("Sequential Time: %.2f ms%n", durationSequential);
        System.out.printf("Parallel Time:   %.2f ms%n", durationParallel);
        if (durationParallel > 0) {
            double speedup = durationSequential / durationParallel;
            System.out.printf("Speedup: %.2fx%n", speedup);
        }
    }

    // Applies all predicates to a single quotation and returns a result object.
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
    
    // (This method is the same as the previous example)
    private static Set<Quotation> findDuplicateCustomerQuotes(List<Quotation> quotes, double threshold) {
        Set<Quotation> duplicateSet = new HashSet<>();
        for (int i = 0; i < quotes.size(); i++) {
            for (int j = i + 1; j < quotes.size(); j++) {
                Quotation q1 = quotes.get(i);
                Quotation q2 = quotes.get(j);
                if (CosineSimilarity.calculate(q1.customer().name(), q2.customer().name()) >= threshold) {
                    duplicateSet.add(q1);
                    duplicateSet.add(q2);
                }
            }
        }
        return duplicateSet;
    }
}