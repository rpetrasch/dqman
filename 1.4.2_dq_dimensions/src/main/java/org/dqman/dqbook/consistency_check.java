package org.dqman.dqbook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Consistency check for orders and customers
 */
public class consistency_check {
    private final static Logger LOGGER = LoggerFactory.getLogger(consistency_check.class);

    public static void main(String[] args) {
        LOGGER.info("Consistency check for orders and customers");

        // create customer list
        List<customer> customers = new ArrayList<>();
        customers.add(new customer(1L, "Alice"));
        customers.add(new customer(2L, "Bob"));
        customers.add(new customer(3L, "Charlie"));
        // create order list
        List<order> orders = new ArrayList<>();
        orders.add(new order(1L, 1L, LocalDate.of(2021, 1, 1)));
        orders.add(new order(2L, 1L, LocalDate.of(2021, 1, 2)));
        orders.add(new order(3L, 4L, LocalDate.of(2021, 1, 3)));
        orders.add(new order(4L, null, LocalDate.of(2021, 1, 3)));

        // check if all orders have a matching customer (naive implementation)
        for (order order : orders) { // loop: for all orders
            boolean customerForOrderFound = false;
            for (customer customer : customers) { // loop: for all customers
                if (customer.getCustomerId().equals(order.getCustomerId())) {
                    customerForOrderFound = true;
                    break;
                }
            }
            if (!customerForOrderFound) {
                LOGGER.error("Order " + order.getOrderId() + ": no matching customer found");
            }
        }
    }
}