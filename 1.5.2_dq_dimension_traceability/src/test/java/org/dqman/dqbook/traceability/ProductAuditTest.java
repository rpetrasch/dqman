package org.dqman.dqbook.traceability;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.dqman.dqbook.traceability.controller.ProductController;
import org.dqman.dqbook.traceability.entity.Product;
import org.dqman.dqbook.traceability.service.AuditService;
import org.dqman.dqbook.traceability.util.DiffUtil;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.annotation.Commit;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.transaction.TestTransaction;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.dqman.dqbook.traceability.util.DiffUtil.diff;

/**
 * Test the product audit history
 */
@SpringBootTest
@ActiveProfiles("test")  // Use test-specific configurations if needed
@Transactional
@Rollback(false) // or use @Commit
@Commit
public class ProductAuditTest {

    public static final Logger LOG = LoggerFactory.getLogger(ProductAuditTest.class);

    @Autowired
    private Environment env;

    @Autowired
    private ProductController productController;

    @Autowired
    private AuditService auditService;

    @Autowired
    private EntityManager entityManager;

    private static String productName;
    private static Double productPrice;
    private static Double productUpdatePrice;

    /**
     * Setup the test data: get product attributes from environment (s. application-test.properties)
     */
    @BeforeEach
    public void setup() {
        productName = env.getProperty("custom.test.product.name");
        productPrice = env.getProperty("custom.test.product.price", Double.class);
        productUpdatePrice = env.getProperty("custom.test.product.update_price", Double.class);
    }

    /**
     * Helper method: Insert a product into the database
     * @return the product that was inserted
     */
    @Transactional
    @Rollback(false) // or use @Commit
    @Commit
    public Product insertProduct() {
        Product product = new Product();
        product.setName(productName);
        product.setPrice(productPrice);
        product = productController.createProduct(product);

        assertThat(product.getId()).isNotNull();
        return product;
    }

    /**
     * Helper method: Update a product in the database
     * @param product to be updated
     * @return updated project
     */
    @Transactional
    @Rollback(false) // or use @Commit
    @Commit
    public Product updatetProduct(Product product) {
        product.setPrice(productUpdatePrice);
        product = productController.createProduct(product);
        return product;
    }

    /**
     * Test the product audit history
     * using the controller methods
     */
    @Test
    @Transactional
    @Commit // or use @Rollback(false)
    public void testProductAudit() {
        //  TestTransaction.start(); // must not do that: Transaction already active
        Product product = insertProduct();
        TestTransaction.flagForCommit();
        TestTransaction.end(); // Commit the product creation

        TestTransaction.start();
        product = updatetProduct(product);
        // entityManager.flush(); // no effect: transaction not committed
        // entityManager.clear();
        TestTransaction.flagForCommit();
        TestTransaction.end(); // Commit the product update

        // Start a new transaction to read the committed data (audit logs)
        TestTransaction.start();

        // Fetch audit history using ProductController (uses the AuditService that uses the AuditReader
        List<Object[]> productAudit = productController.getProductAudit(product.getId());
        showAndCheckAuditHistory(productAudit);

        // Fetch audit history using AuditReader directly
        AuditReader auditReader = AuditReaderFactory.get(entityManager);
        List<Number> revisions = auditReader.getRevisions(Product.class, product.getId());  // Retrieve audit revisions
        LOG.info("Number of revisions: " + revisions.size());
        showAndCheckAuditRevisions(auditReader, product.getId(), revisions);

        TestTransaction.end();
    }

    /**
     * Show and check the audit revisions with the auditReader.
     * Asserts that the values for the revisions are correct
     * @param auditReader
     * @param productId
     * @param revisions
     */
    private void showAndCheckAuditRevisions(AuditReader auditReader, Long productId, List<Number> revisions) {
        assertThat(revisions).hasSize(2);

        // Retrieve both revisions
        Product revision1 = auditReader.find(Product.class, productId, revisions.get(0));
        Product revision2 = auditReader.find(Product.class, productId, revisions.get(1));

        // Check revision 1 (initial creation)
        assertThat(revision1.getPrice()).isEqualTo(productPrice);
        assertThat(revision1.getName()).isEqualTo(productName);

        // Check revision 2 (after update)
        assertThat(revision2.getPrice()).isEqualTo(productUpdatePrice);
    }

    /**
     * Show and check the audit revisions (history) and create a diff of the changes.
     * Asserts that the changes are correct
     * @param productAudit list of audit entries
     */
    private void showAndCheckAuditHistory(List<Object[]> productAudit) {
        assertThat(productAudit).hasSize(2);
        for (Object[] audit : productAudit) {
            LOG.info("Audit: " + audit[0] + " - " + audit[1] + " - " + audit[2]);
        }
        Product productAuditOrg = (Product) productAudit.get(0)[0];
        Product productAuditUpdated = (Product) productAudit.get(1)[0];
        Map<String, DiffUtil.DiffResult> diffs = diff(productAuditOrg, productAuditUpdated);
        diffs.forEach((field, diff) -> {
            LOG.info(field + " changed from " + diff.getOldValue() + " to " + diff.getNewValue());
        });
        assertThat(diffs).hasSize(1);
        assertThat(diffs.get("price").getOldValue()).isEqualTo(productPrice);
        assertThat(diffs.get("price").getNewValue()).isEqualTo(productUpdatePrice);
    }
}
