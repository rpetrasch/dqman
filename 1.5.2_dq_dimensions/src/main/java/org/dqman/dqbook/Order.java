package org.dqman.dqbook;

import java.time.LocalDate;

/**
 * Order entity
 */
public class Order {

    private final Long orderId;
    private final Long customerId;
    private final LocalDate orderDate;

    public Order(Long orderId, Long customerId, LocalDate orderDate) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.orderDate = orderDate;
    }

    public Long getOrderId() {
        return orderId;
    }
    public Long getCustomerId() {
        return customerId;
    }
    public LocalDate getOrderDate() {
        return orderDate;
    }
}
