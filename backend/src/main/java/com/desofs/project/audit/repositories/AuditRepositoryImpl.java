package com.desofs.project.audit.repositories;

import com.desofs.project.audit.mapper.AuditPersistenceMapper;
import com.desofs.project.audit.dbSchema.AuditLogEntity;
import com.desofs.project.audit.model.AuditLog;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditRepositoryImpl implements AuditRepository {

    private final AuditPersistenceMapper persistenceMapper;
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public AuditLog save(AuditLog log) {
        if (log.getId() != null) {
            throw new UnsupportedOperationException("Audit logs are append-only");
        }
        AuditLogEntity entity = persistenceMapper.toEntity(log);
        entityManager.persist(entity);
        return persistenceMapper.toDomain(entity);
    }

    @Override
    public List<AuditLog> findAll() {
        return entityManager.createQuery("SELECT a FROM AuditLogEntity a ORDER BY a.timestamp DESC", AuditLogEntity.class)
                .getResultList()
                .stream()
                .map(persistenceMapper::toDomain)
                .toList();
    }

    @Override
    public List<AuditLog> findByUserId(UUID userId) {
        return entityManager.createQuery(
                        "SELECT a FROM AuditLogEntity a WHERE a.userId = :userId ORDER BY a.timestamp DESC", AuditLogEntity.class)
                .setParameter("userId", userId)
                .getResultList()
                .stream()
                .map(persistenceMapper::toDomain)
                .toList();
    }
}
