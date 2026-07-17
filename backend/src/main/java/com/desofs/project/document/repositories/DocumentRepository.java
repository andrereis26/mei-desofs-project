package com.desofs.project.document.repositories;

import com.desofs.project.document.model.Document;
import com.desofs.project.user.model.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentRepository {
    Document save(Document document);
    List<Document> findAll();
    Optional<Document> findById(UUID id);
    void delete(Document document);
    void deleteAll();
    List<Document> findByOwnerId(UUID ownerId);
    Page<Document> listDocuments(UUID userId, Role role, String filename, UUID departmentId, Pageable pageable);
}
