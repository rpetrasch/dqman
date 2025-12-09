package org.dqman.streamspredicates2;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Find duplicate quotations based on customer name.
 * ToDo: The current version leads to the effect that all quotes are considered duplicates.
 * Exercise: a) What causes this?
 *           b) What are the possible solutions?
 * Hints: Take a look at the debug output for the buckets and check the field that is used to find duplicates.
 */
public class FindDuplicates {

    /**
     * Finds all quotations from customers with similar names.
     * Problem: It runs in quadratic time complexity.
     * @param quotes List of quotations
     * @param threshold Similarity threshold
     * @return Set of duplicate quotations
     */
    public static Set<Quotation> findDuplicateCustomerQuotesNaive(List<Quotation> quotes, double threshold) {
        Set<Quotation> duplicateSet = new HashSet<>();
        // Iterate over all pairs of quotes and check for similarity
        // Note: This is a very naive implementation that runs in O(n^2) time.
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

    /**
     * Finds all quotations from customers with similar names.
     * Advantage: It runs in linear time complexity.
     * @param quotes List of quotations
     * @param threshold Similarity threshold
     * @return Set of duplicate quotations
     */
    public static Set<Quotation> findDuplicateCustomerQuotesBucketing(List<Quotation> quotes, double threshold) {
        Set<Quotation> duplicateSet = new HashSet<>();

        // Group by normalized name (lowercase, trim, remove extra spaces)
        Map<String, List<Quotation>> buckets = quotes.stream()
                .collect(Collectors.groupingBy(q -> normalizeForBucketing(q.customer().name())));

        // Only compare within each bucket
        for (List<Quotation> bucket : buckets.values()) {
            if (bucket.size() > 1) {
                // Now do pairwise comparison only within this small group
                for (int i = 0; i < bucket.size(); i++) {
                    for (int j = i + 1; j < bucket.size(); j++) {
                        if (CosineSimilarity.calculate(
                                bucket.get(i).customer().name(),
                                bucket.get(j).customer().name()) >= threshold) {
                            duplicateSet.add(bucket.get(i));
                            duplicateSet.add(bucket.get(j));
                        }
                    }
                }
            }
        }
        return duplicateSet;
    }

    /**
     * Normalize a name for bucketing.
     * @param name Customer name
     * @return Normalized name
     */
    private static String normalizeForBucketing(String name) {
        return name.toLowerCase()
                .trim()
                .replaceAll("\\s+", " ")
                .replaceAll("[^a-z0-9\\s]", ""); // Remove special chars
    }

    /**
     * Find duplicate quotations in parallel using buckets and streams.
     * @param quotes List of quotations
     * @param threshold Similarity threshold
     * @return Set of duplicate quotations
     */
    public static Set<Quotation> findDuplicateCustomerQuotesBucketingStream(List<Quotation> quotes, double threshold) {
        // Group by normalized name (lowercase, trim, remove extra spaces)
        Map<String, List<Quotation>> buckets = quotes.stream()
                .collect(Collectors.groupingBy(q -> normalizeForBucketing(q.customer().name())));
        // Find duplicates within each bucket
        Set<Quotation> duplicateSet = buckets.values().stream()
                .filter(bucket -> bucket.size() > 1)
                .flatMap(bucket -> findDuplicatesInBucket(bucket, threshold).stream())
                .collect(Collectors.toSet());
        return duplicateSet;
    }

    /**
     * Find duplicate quotations in parallel using buckets and parallel streams.
     * @param quotes List of quotations
     * @param threshold Similarity threshold
     * @return Set of duplicate quotations
     */
    public static Set<Quotation> findDuplicateCustomerQuotesBucketingParallelStream(List<Quotation> quotes, double threshold) {
        // Group by normalized name (lowercase, trim, remove extra spaces)
        Map<String, List<Quotation>> buckets = quotes.stream()
                .collect(Collectors.groupingBy(q -> normalizeForBucketing(q.customer().name())));
        // Find duplicates within each bucket
        Set<Quotation> duplicateSet = buckets.values().parallelStream()
                .filter(bucket -> bucket.size() > 1)
                .flatMap(bucket -> findDuplicatesInBucket(bucket, threshold).stream())
                .collect(Collectors.toSet());
        return duplicateSet;
    }

    /**
     * Find duplicates within one bucket.
     * @param bucket List of quotations in one bucket
     * @param threshold Similarity threshold
     * @return Set of duplicate quotations in this bucket
     */
    private static Set<Quotation> findDuplicatesInBucket(List<Quotation> bucket, double threshold) {
        // Comment out the following line to see the debug output
//        if (bucket.size() > 10) {  // Debug large buckets
//            System.out.println("DEBUG: Large bucket with " + bucket.size() + " quotations");
//            System.out.println("  Sample names: " + bucket.stream()
//                    .limit(5)
//                    .map(q -> q.customer().name())
//                    .collect(Collectors.joining(", ")));
//        }
        Set<Quotation> localDuplicates = new HashSet<>();
        for (int i = 0; i < bucket.size(); i++) {
            for (int j = i + 1; j < bucket.size(); j++) {
                if (CosineSimilarity.calculate(
                        bucket.get(i).customer().name(),
                        bucket.get(j).customer().name()) >= threshold) {
                    localDuplicates.add(bucket.get(i));
                    localDuplicates.add(bucket.get(j));
                }
            }
        }
        return localDuplicates;
    }

}
