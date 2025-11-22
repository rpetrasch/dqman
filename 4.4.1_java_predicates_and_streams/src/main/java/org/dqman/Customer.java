package org.dqman;

import java.math.BigDecimal;

/** Represents a customer with a name and a credit limit.
 *
 * @param id
 * @param name
 * @param maxCreditLine
 */
record Customer(long id, String name, BigDecimal maxCreditLine) {}