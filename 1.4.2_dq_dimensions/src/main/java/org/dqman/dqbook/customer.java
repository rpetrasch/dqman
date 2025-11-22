package org.dqman.dqbook;

/**
 * Customer entity
 */
public class customer {
    private final Long customerId;  // customerId cannot be changed after creation
    private String name;

    public customer(Long customerId, String name) {
        this.customerId = customerId;
        this.name = name;
    }

    public Long getCustomerId() {
        return customerId;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
}
