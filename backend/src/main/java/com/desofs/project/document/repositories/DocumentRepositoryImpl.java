package com.desofs.project.document.repositories;

import com.desofs.project.department.dbSchema.DepartmentEntity;
import com.desofs.project.document.mapper.DocumentPersistenceMapper;
import com.desofs.project.document.dbSchema.DocumentEntity;
import com.desofs.project.document.model.Document;
import com.desofs.project.shared.persistence.LikePatternEscaper;
import com.desofs.project.user.dbSchema.UserEntity;
import com.desofs.project.user.model.Role;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DocumentRepositoryImpl implements DocumentRepository {

    private final DocumentPersistenceMapper persistenceMapper;
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public Document save(Document document) {
        DocumentEntity entity = persistenceMapper.toEntity(document);
        DocumentEntity saved;
        if (entity.getId() == null) {
            entityManager.persist(entity);
            saved = entity;
        } else {
            saved = entityManager.merge(entity);
        }
        return persistenceMapper.toDomain(saved);
    }

    @Override
    public List<Document> findAll() {
        return entityManager.createQuery("SELECT d FROM DocumentEntity d", DocumentEntity.class)
                .getResultList()
                .stream()
                .map(persistenceMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Document> findById(UUID id) {
        return Optional.ofNullable(entityManager.find(DocumentEntity.class, id)).map(persistenceMapper::toDomain);
    }

    @Override
    @Transactional
    public void delete(Document document) {
        DocumentEntity entity = entityManager.find(DocumentEntity.class, document.getId());
        if (entity != null) {
            entityManager.remove(entity);
        }
    }

    @Override
    @Transactional
    public void deleteAll() {
        for (DocumentEntity entity : entityManager.createQuery("SELECT d FROM DocumentEntity d", DocumentEntity.class).getResultList()) {
            entityManager.remove(entity);
        }
    }

    @Override
    public List<Document> findByOwnerId(UUID ownerId) {
        return entityManager.createQuery(
                        "SELECT d FROM DocumentEntity d WHERE d.owner.id = :ownerId", DocumentEntity.class)
                .setParameter("ownerId", ownerId)
                .getResultList()
                .stream()
                .map(persistenceMapper::toDomain)
                .toList();
    }

    @Override
    public Page<Document> listDocuments(UUID userId, Role role, String filename, UUID departmentId, Pageable pageable) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<DocumentEntity> criteria = criteriaBuilder.createQuery(DocumentEntity.class);
        Root<DocumentEntity> document = criteria.from(DocumentEntity.class);

        List<Predicate> predicates = buildDocumentPredicates(
                criteriaBuilder, criteria, document, userId, role, filename, departmentId);

        criteria.select(document)
                .where(predicates.toArray(Predicate[]::new))
                .orderBy(criteriaBuilder.desc(document.get("createdAt")),
                        criteriaBuilder.asc(document.get("filename")));

        TypedQuery<DocumentEntity> query = entityManager.createQuery(criteria)
                .setFirstResult(Math.toIntExact(pageable.getOffset()))
                .setMaxResults(pageable.getPageSize());

        List<Document> content = query.getResultList()
                .stream()
                .map(persistenceMapper::toDomain)
                .toList();

        return new PageImpl<>(content, pageable, countDocuments(userId, role, filename, departmentId));
    }

    private long countDocuments(UUID userId, Role role, String filename, UUID departmentId) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> criteria = criteriaBuilder.createQuery(Long.class);
        Root<DocumentEntity> document = criteria.from(DocumentEntity.class);

        List<Predicate> predicates = buildDocumentPredicates(
                criteriaBuilder, criteria, document, userId, role, filename, departmentId);

        criteria.select(criteriaBuilder.count(document))
                .where(predicates.toArray(Predicate[]::new));

        return entityManager.createQuery(criteria).getSingleResult();
    }

    private List<Predicate> buildDocumentPredicates(
            CriteriaBuilder criteriaBuilder,
            CriteriaQuery<?> criteria,
            Root<DocumentEntity> document,
            UUID userId,
            Role role,
            String filename,
            UUID departmentId) {
        List<Predicate> predicates = new ArrayList<>();

        if (role != Role.ADMIN) {
            predicates.add(buildAccessPredicate(criteriaBuilder, criteria, document, userId, role));
        }

        if (filename != null && !filename.isBlank()) {
            predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(document.get("filename")),
                    LikePatternEscaper.containsIgnoreCase(filename),
                    LikePatternEscaper.ESCAPE_CHARACTER));
        }

        if (departmentId != null) {
            predicates.add(criteriaBuilder.equal(document.get("department").get("id"), departmentId));
        }

        return predicates;
    }

    private Predicate buildAccessPredicate(
            CriteriaBuilder criteriaBuilder,
            CriteriaQuery<?> criteria,
            Root<DocumentEntity> document,
            UUID userId,
            Role role) {
        Predicate ownerAccess = criteriaBuilder.equal(document.get("owner").get("id"), userId);

        if (role != Role.MANAGER) {
            return ownerAccess;
        }

        Predicate departmentAccess = criteriaBuilder.and(
                criteriaBuilder.isNotNull(document.get("department")),
                criteriaBuilder.or(
                        existsManagedDepartment(criteriaBuilder, criteria, document, userId),
                        existsMemberDepartment(criteriaBuilder, criteria, document, userId)
                ));

        return criteriaBuilder.or(ownerAccess, departmentAccess);
    }

    private Predicate existsManagedDepartment(
            CriteriaBuilder criteriaBuilder,
            CriteriaQuery<?> criteria,
            Root<DocumentEntity> document,
            UUID userId) {
        Subquery<UUID> subquery = criteria.subquery(UUID.class);
        Root<DepartmentEntity> department = subquery.from(DepartmentEntity.class);
        Join<DepartmentEntity, UserEntity> manager = department.join("managers");

        subquery.select(department.get("id"))
                .where(
                        criteriaBuilder.equal(department.get("id"), document.get("department").get("id")),
                        criteriaBuilder.equal(manager.get("id"), userId)
                );

        return criteriaBuilder.exists(subquery);
    }

    private Predicate existsMemberDepartment(
            CriteriaBuilder criteriaBuilder,
            CriteriaQuery<?> criteria,
            Root<DocumentEntity> document,
            UUID userId) {
        Subquery<UUID> subquery = criteria.subquery(UUID.class);
        Root<UserEntity> user = subquery.from(UserEntity.class);
        Join<UserEntity, DepartmentEntity> department = user.join("departments");

        subquery.select(user.get("id"))
                .where(
                        criteriaBuilder.equal(user.get("id"), userId),
                        criteriaBuilder.equal(department.get("id"), document.get("department").get("id"))
                );

        return criteriaBuilder.exists(subquery);
    }
}
