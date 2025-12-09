package org.dqman.streamspredicates2;

import java.math.BigDecimal;

/**
 * Represents a customer with a name and a credit limit.
 * This class is needed for the quotation (many-to-one relationship: 1 customer has many quotations).
 * The relationship is defined in the Quotation class (unidirectional), so that the customer class
 * is not dependent on the quotation class. This has several benefits:
 *
 * a) Dependency Inversion Principle (DIP)
 *    The higher-level Customer class doesn't depend on the lower-level Quotation class.
 *    Dependencies flow in one direction: Quotation → Customer.
 * b) Acyclic Dependencies Principle (ADP)
 *    By making the relationship unidirectional, you avoid circular dependencies between Customer and Quotation.
 *    Circular dependencies create tight coupling and make code harder to maintain, test, and understand.
 * c) Stable Dependencies Principle (SDP)
 *    Customer is likely a more stable, core domain concept that changes less frequently.
 *    Quotation is more volatile (business rules around quotations might change often). Making Quotation depend on Customer (not vice versa) means changes to quotations won't ripple back to affect the customer class.
 * d) Single Responsibility Principle (SRP)
 *    Customer focuses on customer-related concerns without needing to know about quotations.
 *    If we need to work with a customer's quotations, that logic lives elsewhere (perhaps in a repository or service layer).
 * e) Low Coupling / High Cohesion
 *    This design minimizes coupling between classes. Customer can exist and be tested independently of Quotation.
 * Questions: a) Why not use a class instead of a record?
 *            b) What if we have a customer and need to know all quotations associated with this customer?
 *               There is no direct relationship between Customer and Quotation.
 * @param id
 * @param name
 * @param maxCreditLine
 */
public record Customer(long id, String name, BigDecimal maxCreditLine) {
    public static final int MAX_CREDIT_LINE_INT = 10000;
    public static final BigDecimal MAX_CREDIT_LINE = BigDecimal.valueOf(MAX_CREDIT_LINE_INT);
}