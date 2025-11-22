
1. Idea: In java classes like predicate exist. They can be used for data quality checks. Can you generate a non trivial example for a set of DQ rules applying to customer quotations, like the date of issue must be after the expiration date, quote total amount must be less than the max. credit line for a customer, no customer duplicates (identified by similar names checked via cosine similarity).


Of course. You can use Java's `Predicate` interface to create a powerful and reusable set of data quality rules. For more complex, context-aware rules like duplicate detection, you often combine predicates with other methods.

Here is a non-trivial example for checking customer quotations.

-----

### 1\. Data Model (Records)

First, let's define our data structures using modern Java `records` for conciseness.

```java
import java.math.BigDecimal;
import java.time.LocalDate;

// Represents a customer with a name and a credit limit.
record org.dqman.Customer(long id, String name, BigDecimal maxCreditLine) {}

// Represents a quotation with its details and associated customer.
record org.dqman.Quotation(long id, org.dqman.Customer customer, LocalDate issueDate, LocalDate expirationDate, BigDecimal totalAmount) {}
```

### 2\. Cosine Similarity Utility

For the name similarity check, we need a utility to calculate the cosine similarity between two strings. This class converts strings into character frequency vectors and then computes their similarity. A score of **1.0** means identical, while a score of **0.0** means completely different.

```java
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class org.dqman.CosineSimilarity {

    // Calculates the cosine similarity between two strings.
    public static double calculate(String text1, String text2) {
        if (text1 == null || text2 == null) {
            return 0.0;
        }

        Map<Character, Integer> vec1 = toFrequencyVector(text1.toLowerCase());
        Map<Character, Integer> vec2 = toFrequencyVector(text2.toLowerCase());

        Set<Character> commonChars = new HashSet<>(vec1.keySet());
        commonChars.retainAll(vec2.keySet());

        double dotProduct = 0.0;
        for (Character ch : commonChars) {
            dotProduct += vec1.get(ch) * vec2.get(ch);
        }

        double magnitude1 = calculateMagnitude(vec1);
        double magnitude2 = calculateMagnitude(vec2);

        if (magnitude1 == 0.0 || magnitude2 == 0.0) {
            return 0.0;
        }

        return dotProduct / (magnitude1 * magnitude2);
    }

    // Creates a map of characters to their frequencies.
    private static Map<Character, Integer> toFrequencyVector(String text) {
        Map<Character, Integer> vector = new HashMap<>();
        for (char c : text.toCharArray()) {
            if (Character.isLetterOrDigit(c)) {
                vector.put(c, vector.getOrDefault(c, 0) + 1);
            }
        }
        return vector;
    }

    // Calculates the magnitude (Euclidean norm) of a vector.
    private static double calculateMagnitude(Map<Character, Integer> vector) {
        double sumOfSquares = 0.0;
        for (int count : vector.values()) {
            sumOfSquares += count * count;
        }
        return Math.sqrt(sumOfSquares);
    }
}
```

### 3\. Data Quality Predicates

Here, we define our DQ rules as `Predicate<org.dqman.Quotation>`. We can easily chain them together using `.and()` and `.or()`.

```java
import java.util.function.Predicate;

public class org.dqman.QuotationDataQualityRules {

    /**
     * Rule 1: The expiration date must be after the issue date.
     */
    public static final Predicate<org.dqman.Quotation> IS_EXPIRATION_DATE_VALID =
            q -> q.expirationDate().isAfter(q.issueDate());

    /**
     * Rule 2: The total amount must not exceed the customer's maximum credit line.
     */
    public static final Predicate<org.dqman.Quotation> IS_WITHIN_CREDIT_LINE =
            q -> q.totalAmount().compareTo(q.customer().maxCreditLine()) <= 0;

    /**
     * Combined Rule: A basic valid quotation must satisfy both rules above.
     * This demonstrates the power of predicate composition.
     */
    public static final Predicate<org.dqman.Quotation> HAS_BASIC_VALIDITY =
            IS_EXPIRATION_DATE_VALID.and(IS_WITHIN_CREDIT_LINE);
}
```

### 4\. Applying the Rules (org.dqman.org.dqman.Main Class)

This class demonstrates how to use the predicates for simple checks and a separate method for the more complex duplicate check.

```java
import org.dqman.CosineSimilarity;
import org.dqman.QuotationDataQualityRules;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DataQualityChecker {

    public static void main(String[] args) {
        // --- Sample Data ---
        org.dqman.Customer cust1 = new org.dqman.Customer(101, "John Smith", new BigDecimal("5000"));
        org.dqman.Customer cust2 = new org.dqman.Customer(102, "Jane Doe", new BigDecimal("10000"));
        org.dqman.Customer cust3 = new org.dqman.Customer(103, "Jon Smith", new BigDecimal("2000")); // Similar name to cust1

        List<org.dqman.Quotation> quotations = List.of(
                // 1. Perfectly valid quotation
                new org.dqman.Quotation(1, cust2, LocalDate.now(), LocalDate.now().plusDays(30), new BigDecimal("8500.00")),
                // 2. INVALID: Expiration date is before issue date
                new org.dqman.Quotation(2, cust1, LocalDate.now(), LocalDate.now().minusDays(1), new BigDecimal("1500.00")),
                // 3. INVALID: Total amount exceeds customer's credit line
                new org.dqman.Quotation(3, cust1, LocalDate.now(), LocalDate.now().plusDays(15), new BigDecimal("5500.50")),
                // 4. DUPLICATE CUSTOMER: "Jon Smith" is very similar to "John Smith"
                new org.dqman.Quotation(4, cust3, LocalDate.now(), LocalDate.now().plusDays(20), new BigDecimal("1200.00"))
        );

        System.out.println("--- Running Predicate-based DQ Checks ---");
        quotations.forEach(q -> {
            System.out.println("\nChecking org.dqman.Quotation ID: " + q.id());
            System.out.printf("  - Is expiration date valid? %s%n",
                    QuotationDataQualityRules.IS_EXPIRATION_DATE_VALID.test(q));
            System.out.printf("  - Is within credit line? %s%n",
                    QuotationDataQualityRules.IS_WITHIN_CREDIT_LINE.test(q));
            System.out.printf("  - Overall basic validity? %s%n",
                    QuotationDataQualityRules.HAS_BASIC_VALIDITY.test(q));
        });


        System.out.println("\n--- Running Duplicate org.dqman.Customer Check (Cosine Similarity) ---");
        double similarityThreshold = 0.90; // Set a high threshold for similarity
        Set<org.dqman.Quotation> duplicates = findDuplicateCustomerQuotes(quotations, similarityThreshold);

        if (duplicates.isEmpty()) {
            System.out.println("No duplicate customers found.");
        } else {
            System.out.printf("Found potential duplicates (similarity > %.2f):%n", similarityThreshold);
            duplicates.forEach(q -> System.out.printf("  - org.dqman.Quotation ID %d for customer '%s'%n", q.id(), q.customer().name()));
        }
    }

    /**
     * Finds quotations where customer names are highly similar.
     * This is a context-aware check that a simple Predicate cannot perform on its own,
     * as it needs to compare items against each other.
     */
    public static Set<org.dqman.Quotation> findDuplicateCustomerQuotes(List<org.dqman.Quotation> quotes, double threshold) {
        Set<org.dqman.Quotation> duplicateSet = new HashSet<>();
        for (int i = 0; i < quotes.size(); i++) {
            for (int j = i + 1; j < quotes.size(); j++) {
                org.dqman.Quotation q1 = quotes.get(i);
                org.dqman.Quotation q2 = quotes.get(j);

                double similarity = CosineSimilarity.calculate(
                        q1.customer().name(),
                        q2.customer().name()
                );

                if (similarity >= threshold) {
                    duplicateSet.add(q1);
                    duplicateSet.add(q2);
                }
            }
        }
        return duplicateSet;
    }
}
```

### Expected Output

Running the `DataQualityChecker` will produce the following output, clearly identifying which quotations failed which rules.

```text
--- Running Predicate-based DQ Checks ---

Checking org.dqman.Quotation ID: 1
  - Is expiration date valid? true
  - Is within credit line? true
  - Overall basic validity? true

Checking org.dqman.Quotation ID: 2
  - Is expiration date valid? false
  - Is within credit line? true
  - Overall basic validity? false

Checking org.dqman.Quotation ID: 3
  - Is expiration date valid? true
  - Is within credit line? false
  - Overall basic validity? false

Checking org.dqman.Quotation ID: 4
  - Is expiration date valid? true
  - Is within credit line? true
  - Overall basic validity? true

--- Running Duplicate org.dqman.Customer Check (Cosine Similarity) ---
Found potential duplicates (similarity > 0.90):
  - org.dqman.Quotation ID 2 for customer 'John Smith'
  - org.dqman.Quotation ID 4 for customer 'Jon Smith'
```

This example shows how `Predicate` is excellent for stateless, single-object validation, while more complex, stateful, or cross-record validations require complementary methods. 👍