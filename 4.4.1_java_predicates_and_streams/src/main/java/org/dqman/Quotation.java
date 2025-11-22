package org.dqman;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Represents a quotation with its details and associated customer.
 *
 * @param id
 * @param customer
 * @param issueDate
 * @param expirationDate
 * @param totalAmount
 */
record Quotation(long id, Customer customer, LocalDate issueDate, LocalDate expirationDate, BigDecimal totalAmount) {}