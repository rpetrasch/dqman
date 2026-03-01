package org.dqman.dqbook.traceability.controller;

import jakarta.transaction.Transactional;
import org.dqman.dqbook.traceability.service.AuditService;
import org.dqman.dqbook.traceability.repo.ProductRepo;
import org.dqman.dqbook.traceability.entity.Product;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for products (REST API)
 */
@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductRepo productRepository;
    private final AuditService auditService;

    /**
     * Constructor with injection of the product repository and audit service
     * @param productRepository
     * @param auditService
     */
    public ProductController(ProductRepo productRepository, AuditService auditService) {
        this.productRepository = productRepository;
        this.auditService = auditService;
    }

    /**
     * Create a new product
     * @param product to be created
     * @return saved product
     */
    @PostMapping
    @Transactional
    public Product createProduct(@RequestBody Product product) {
        return productRepository.save(product);
    }

    /**
     * Update a product in the database
     * @param id of the product
     * @param updatedProduct product with new data
     * @return updated product (after saving in database)
     */
    @PutMapping("/{id}")
    @Transactional
    public Product updateProduct(@PathVariable Long id, @RequestBody Product updatedProduct) {
        Product product = productRepository.findById(id).orElseThrow();
        product.setName(updatedProduct.getName());
        product.setPrice(updatedProduct.getPrice());
        return productRepository.save(product);
    }

    /**
     * Get all products
     * @return list of products or empty list
     */
    @GetMapping
    public List<Product> getProducts() {
        return productRepository.findAll();
    }

    /**
     * Get audit entries for a product by id
     * @param id product id
     * @return list of audit entries
     */
    @GetMapping("/{id}/audit")
    public List<Object[]> getProductAudit(@PathVariable Long id) {
        return auditService.getProductAudit(id);
    }
}

