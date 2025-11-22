package org.dqman.dqbook.traceability.repo;

import org.dqman.dqbook.traceability.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for the Product entity.
 */
public interface ProductRepo extends JpaRepository<Product, Long> {
}
