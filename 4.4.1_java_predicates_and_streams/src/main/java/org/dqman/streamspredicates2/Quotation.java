package org.dqman.streamspredicates2;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Represents a quotation with its details and associated customer.
 * This class is references a customer (many-to-one relationship: 1 customer has many quotations).
 *
 * @param id id of the quotation
 * @param customer associated customer
 * @param issueDate date when the quotation was issued
 * @param expirationDate date when the quotation expires
 * @param totalAmount total amount of the quotation
 */
public record Quotation(long id, Customer customer, LocalDate issueDate, LocalDate expirationDate, BigDecimal totalAmount) {}