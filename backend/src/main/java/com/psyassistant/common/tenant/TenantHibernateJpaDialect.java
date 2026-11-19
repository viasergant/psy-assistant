package com.psyassistant.common.tenant;

import jakarta.persistence.EntityManager;
import java.sql.SQLException;
import java.util.UUID;
import org.hibernate.Session;
import org.springframework.orm.jpa.vendor.HibernateJpaDialect;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;

/**
 * Activates the Hibernate {@code orgFilter} on every new transaction.
 *
 * <p>Spring's {@link org.springframework.orm.jpa.JpaTransactionManager} calls
 * {@link #beginTransaction} immediately after binding the Hibernate {@link Session}
 * to the current transaction, so {@code entityManager.unwrap(Session.class)} is
 * always valid at this point. The filter restricts all queries on
 * {@code @Filter(name = "orgFilter")}-annotated entities to rows whose
 * {@code org_id} matches the caller's organization.
 *
 * <p>When no {@code orgId} is present in {@link TenantContext} (e.g. during
 * self-service registration or bootstrapping), the filter is not enabled and all
 * rows are visible — this is the intended behaviour for org-creation flows.
 */
public class TenantHibernateJpaDialect extends HibernateJpaDialect {

    @Override
    public Object beginTransaction(final EntityManager entityManager,
                                   final TransactionDefinition definition)
            throws SQLException, TransactionException {
        Object transactionData = super.beginTransaction(entityManager, definition);
        UUID orgId = TenantContext.get();
        if (orgId != null) {
            Session session = entityManager.unwrap(Session.class);
            if (session.getEnabledFilter("orgFilter") == null) {
                session.enableFilter("orgFilter").setParameter("orgId", orgId);
            }
        }
        return transactionData;
    }
}
