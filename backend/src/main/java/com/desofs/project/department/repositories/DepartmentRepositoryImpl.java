package com.desofs.project.department.repositories;

import com.desofs.project.department.mapper.DepartmentPersistenceMapper;
import com.desofs.project.department.dbSchema.DepartmentEntity;
import com.desofs.project.department.model.Department;
import com.desofs.project.shared.persistence.LikePatternEscaper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DepartmentRepositoryImpl implements DepartmentRepository {

    private static final String LIKE_ESCAPE_CLAUSE = " ESCAPE '" + LikePatternEscaper.ESCAPE_CHARACTER + "'";

    private final DepartmentPersistenceMapper persistenceMapper;
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<Department> findByName(String name) {
        List<DepartmentEntity> results = entityManager.createQuery(
                        "SELECT d FROM DepartmentEntity d WHERE d.name = :name", DepartmentEntity.class)
                .setParameter("name", name)
                .setMaxResults(1)
                .getResultList();
        return results.stream().findFirst().map(persistenceMapper::toDomain);
    }

    @Override
    public boolean existsByName(String name) {
        Long count = entityManager.createQuery(
                        "SELECT COUNT(d) FROM DepartmentEntity d WHERE d.name = :name", Long.class)
                .setParameter("name", name)
                .getSingleResult();
        return count > 0;
    }

    @Override
    @Transactional
    public Department save(Department department) {
        DepartmentEntity entity = persistenceMapper.toEntity(department);
        DepartmentEntity saved;
        if (entity.getId() == null) {
            entityManager.persist(entity);
            saved = entity;
        } else {
            saved = entityManager.merge(entity);
        }
        return persistenceMapper.toDomain(saved);
    }

    @Override
    public List<Department> findAll() {
        return entityManager.createQuery("SELECT d FROM DepartmentEntity d", DepartmentEntity.class)
                .getResultList()
                .stream()
                .map(persistenceMapper::toDomain)
                .toList();
    }

    @Override
    public Page<Department> listDepartments(String name, Pageable pageable) {
        boolean hasNameFilter = name != null && !name.isBlank();
        String jpql = hasNameFilter
                ? "SELECT d FROM DepartmentEntity d WHERE LOWER(d.name) LIKE :name"
                + LIKE_ESCAPE_CLAUSE
                + " ORDER BY d.name ASC"
                : "SELECT d FROM DepartmentEntity d ORDER BY d.name ASC";

        TypedQuery<DepartmentEntity> query = entityManager.createQuery(
                        jpql, DepartmentEntity.class)
                .setFirstResult(Math.toIntExact(pageable.getOffset()))
                .setMaxResults(pageable.getPageSize());

        if (hasNameFilter) {
            query.setParameter("name", LikePatternEscaper.containsIgnoreCase(name));
        }

        List<Department> content = query.getResultList()
                .stream()
                .map(persistenceMapper::toDomain)
                .toList();

        return new PageImpl<>(content, pageable, countDepartments(name));
    }

    @Override
    public Optional<Department> findById(UUID id) {
        return Optional.ofNullable(entityManager.find(DepartmentEntity.class, id)).map(persistenceMapper::toDomain);
    }

    @Override
    @Transactional
    public void delete(Department department) {
        DepartmentEntity entity = entityManager.find(DepartmentEntity.class, department.getId());
        if (entity != null) {
            entityManager.remove(entity);
        }
    }

    @Override
    @Transactional
    public void deleteAll() {
        for (DepartmentEntity entity : entityManager.createQuery("SELECT d FROM DepartmentEntity d", DepartmentEntity.class).getResultList()) {
            entityManager.remove(entity);
        }
    }

    private long countDepartments(String name) {
        boolean hasNameFilter = name != null && !name.isBlank();
        String jpql = hasNameFilter
                ? "SELECT COUNT(d) FROM DepartmentEntity d WHERE LOWER(d.name) LIKE :name"
                + LIKE_ESCAPE_CLAUSE
                : "SELECT COUNT(d) FROM DepartmentEntity d";

        TypedQuery<Long> query = entityManager.createQuery(jpql, Long.class);

        if (hasNameFilter) {
            query.setParameter("name", LikePatternEscaper.containsIgnoreCase(name));
        }

        return query.getSingleResult();
    }
}
