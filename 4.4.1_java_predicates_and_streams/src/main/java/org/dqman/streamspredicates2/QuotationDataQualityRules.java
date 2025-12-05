package org.dqman.streamspredicates2;

import java.util.function.Predicate;

/**
 * This class contains predefined data quality rules for quotations.
 */
public class QuotationDataQualityRules {

    /**
     * Rule 1: The expiration date must be after the issue date.
     */
    public static final Predicate<Quotation> IS_EXPIRATION_DATE_VALID =
            q -> q.expirationDate().isAfter(q.issueDate());

    /**
     * Rule 2: The total amount must not exceed the customer's maximum credit line.
     */
    public static final Predicate<Quotation> IS_WITHIN_CREDIT_LINE =
            q -> q.totalAmount().compareTo(q.customer().maxCreditLine()) <= 0;

    /**
     * Combined Rule: A basic valid quotation must satisfy both rules above.
     * This demonstrates the power of predicate composition.
     */
    public static final Predicate<Quotation> HAS_BASIC_VALIDITY =
            IS_EXPIRATION_DATE_VALID.and(IS_WITHIN_CREDIT_LINE);
}