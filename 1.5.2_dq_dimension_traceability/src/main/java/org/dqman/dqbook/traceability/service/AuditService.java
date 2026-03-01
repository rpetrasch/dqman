package org.dqman.dqbook.traceability.service;

import jakarta.transaction.Transactional;
import org.dqman.dqbook.traceability.entity.Product;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.query.AuditEntity;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

/**
 * Service to retrieve audit information for products
 */
@Service
@Transactional
public class AuditService {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Get the audit history for a product
     * @param productId id of the product
     * @return list of audit entries
     */
    public List<Object[]> getProductAudit(Long productId) {
        AuditReader auditReader = AuditReaderFactory.get(entityManager);
        return auditReader.createQuery()
                .forRevisionsOfEntity(Product.class, false, true)
                .add(AuditEntity.id().eq(productId))
                .getResultList();
    }
}

