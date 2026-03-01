package org.dqman.dqbook.traceability.component;

import jakarta.transaction.Transactional;
import org.dqman.dqbook.traceability.entity.Product;
import org.dqman.dqbook.traceability.repo.ProductRepo;
import org.dqman.dqbook.traceability.service.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Application ready listener for tests of the product audit history
 */
@Component
public class AppReadyListener implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(AppReadyListener.class);
    @Autowired
    private ProductRepo productRepo;

    @Autowired
    private AuditService auditService;

    /**
     * Run post-init code when the application is ready.
     * Creation und update of a product and check if the audit (size) is correct
     * @param event
     */
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        LOG.info("Application is ready. Running post-init code: upsert product...");
        Product product = upsertProduct();
        LOG.info("Product id: " + product.getId());
        List<Object[]> productAudit = auditService.getProductAudit(product.getId());
        LOG.info("Product audits: " + productAudit.size());
    }

    /**
     * Helper method to create and update a product
     * @return product with updated price
     */
    @Transactional
    public Product upsertProduct() {

        // 1. Create and save a product
        Product product = new Product();
        product.setName("Laptop");
        product.setPrice(1200.0);
        product = productRepo.save(product);
        // entityManager.flush();

        // 2. Update the product
        product.setPrice(1300.0);
        product = productRepo.save(product);
        // entityManager.flush();

        return product;
    }
}
