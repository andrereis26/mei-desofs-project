package com.desofs.project.user.repositories;

import com.desofs.project.user.mapper.UserPersistenceMapper;
import com.desofs.project.user.dbSchema.UserEntity;
import com.desofs.project.user.model.Role;
import com.desofs.project.user.model.User;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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
public class UserRepositoryImpl implements UserRepository {

    private final UserPersistenceMapper persistenceMapper;
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<User> findByUsername(String username) {
        List<UserEntity> results = entityManager.createQuery(
                        "SELECT u FROM UserEntity u WHERE u.username = :username", UserEntity.class)
                .setParameter("username", username)
                .setMaxResults(1)
                .getResultList();
        return results.stream().findFirst().map(persistenceMapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        List<UserEntity> results = entityManager.createQuery(
                        "SELECT u FROM UserEntity u WHERE u.email = :email", UserEntity.class)
                .setParameter("email", email)
                .setMaxResults(1)
                .getResultList();
        return results.stream().findFirst().map(persistenceMapper::toDomain);
    }

    @Override
    public boolean existsByUsername(String username) {
        Long count = entityManager.createQuery(
                        "SELECT COUNT(u) FROM UserEntity u WHERE u.username = :username", Long.class)
                .setParameter("username", username)
                .getSingleResult();
        return count > 0;
    }

    @Override
    public boolean existsByEmail(String email) {
        Long count = entityManager.createQuery(
                        "SELECT COUNT(u) FROM UserEntity u WHERE u.email = :email", Long.class)
                .setParameter("email", email)
                .getSingleResult();
        return count > 0;
    }

    @Override
    @Transactional
    public User save(User user) {
        UserEntity entity = persistenceMapper.toEntity(user);
        UserEntity saved;
        if (entity.getId() == null) {
            entityManager.persist(entity);
            saved = entity;
        } else {
            saved = entityManager.merge(entity);
        }
        return persistenceMapper.toDomain(saved);
    }

    @Override
    public List<User> findAll() {
        return entityManager.createQuery("SELECT u FROM UserEntity u", UserEntity.class)
                .getResultList()
                .stream()
                .map(persistenceMapper::toDomain)
                .toList();
    }

    @Override
    public Page<User> listUsers(Role role, Pageable pageable) {
        String jpql = role == null
                ? "SELECT u FROM UserEntity u ORDER BY u.createdAt DESC, u.username ASC"
                : "SELECT u FROM UserEntity u WHERE u.role = :role ORDER BY u.username ASC";

        TypedQuery<UserEntity> query = entityManager.createQuery(jpql, UserEntity.class)
                .setFirstResult(Math.toIntExact(pageable.getOffset()))
                .setMaxResults(pageable.getPageSize());

        if (role != null) {
            query.setParameter("role", role);
        }

        List<User> content = query.getResultList()
                .stream()
                .map(persistenceMapper::toDomain)
                .toList();

        return new PageImpl<>(content, pageable, countUsers(role));
    }

    private long countUsers(Role role) {
        String jpql = role == null
                ? "SELECT COUNT(u) FROM UserEntity u"
                : "SELECT COUNT(u) FROM UserEntity u WHERE u.role = :role";

        TypedQuery<Long> query = entityManager.createQuery(jpql, Long.class);

        if (role != null) {
            query.setParameter("role", role);
        }

        return query.getSingleResult();
    }

    @Override
    public Optional<User> findById(UUID id) {
        return Optional.ofNullable(entityManager.find(UserEntity.class, id)).map(persistenceMapper::toDomain);
    }

    @Override
    public List<User> findAllById(Iterable<UUID> ids) {
        List<UUID> idList = new ArrayList<>();
        ids.forEach(idList::add);
        if (idList.isEmpty()) {
            return List.of();
        }
        return entityManager.createQuery("SELECT u FROM UserEntity u WHERE u.id IN :ids", UserEntity.class)
                .setParameter("ids", idList)
                .getResultList()
                .stream()
                .map(persistenceMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void deleteAll() {
        for (UserEntity entity : entityManager.createQuery("SELECT u FROM UserEntity u", UserEntity.class).getResultList()) {
            entityManager.remove(entity);
        }
    }
}
