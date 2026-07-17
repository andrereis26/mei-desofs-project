package com.desofs.project.document.services;

import com.desofs.project.audit.services.IAuditService;
import com.desofs.project.department.model.Department;
import com.desofs.project.department.repositories.DepartmentRepository;
import com.desofs.project.document.dtos.DocumentDto;
import com.desofs.project.document.mapper.DocumentDtoMapper;
import com.desofs.project.document.model.Document;
import com.desofs.project.document.repositories.DocumentRepository;
import com.desofs.project.shared.exceptions.DepartmentNotFoundException;
import com.desofs.project.shared.exceptions.DocumentNotFoundException;
import com.desofs.project.shared.exceptions.EmptyFileException;
import com.desofs.project.shared.exceptions.UserNotFoundException;
import com.desofs.project.infrastructure.filesystem.IFileStorageService;
import com.desofs.project.shared.dtos.PageResponse;
import com.desofs.project.user.model.Role;
import com.desofs.project.user.model.User;
import com.desofs.project.user.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService implements IDocumentService {

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final IFileStorageService fileStorageService;
    private final IAuditService auditService;
    private final DocumentDtoMapper documentDtoMapper;
    private final DocumentUploadValidationService documentUploadValidationService;

    @Value("${app.file-storage.path:./uploads}")
    private String storagePath;

    @Override
    @Transactional
    public DocumentDto upload(MultipartFile file, String username, UUID departmentId) throws IOException {
        if (file.isEmpty()) {
            throw new EmptyFileException();
        }
        User owner = userRepository.findByUsername(username)
                .orElseThrow(UserNotFoundException::new);

        Department department = resolveUploadDepartment(owner, departmentId);
        ValidatedUpload validatedUpload = documentUploadValidationService.validate(file);

        String userDir = storagePath + "/" + owner.getId();
        fileStorageService.createStorageDirectory(userDir);

        String originalFilename = sanitizeFilename(file.getOriginalFilename());
        String storedFilename = UUID.randomUUID() + "_" + originalFilename;
        String filepath = fileStorageService.storeFile(userDir, storedFilename, validatedUpload.content());

        Document document = Document.builder()
                .filename(originalFilename)
                .filepath(filepath)
            .contentType(validatedUpload.detectedContentType())
            .size((long) validatedUpload.content().length)
                .owner(owner)
                .department(department)
                .build();

        Document saved = documentRepository.save(document);
        auditService.log("UPLOAD", "Document", saved.getId().toString(), owner.getId(),
                "File uploaded: " + originalFilename);
        log.info("Document uploaded. documentId={}, ownerId={}, departmentId={}",
            saved.getId(), owner.getId(), department == null ? "none" : department.getId());
        return documentDtoMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentDto getDocument(UUID id, String username) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new DocumentNotFoundException(id));

        User user = userRepository.findByUsername(username)
                .orElseThrow(UserNotFoundException::new);

        if (!canAccessDocument(user, document)) {
            throw new AccessDeniedException("Access denied");
        }

        return documentDtoMapper.toDto(document);
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentDownload downloadDocument(UUID id, String username) throws IOException {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new DocumentNotFoundException(id));

        User user = userRepository.findByUsername(username)
                .orElseThrow(UserNotFoundException::new);

        if (!canAccessDocument(user, document)) {
            throw new AccessDeniedException("Access denied");
        }

        auditService.log("DOWNLOAD", "Document", document.getId().toString(), user.getId(),
                "Document downloaded: " + document.getFilename());
        log.info("Document downloaded. documentId={}, userId={}", document.getId(), user.getId());

        return new DocumentDownload(
                fileStorageService.readFile(document.getFilepath()),
                document.getContentType(),
                document.getFilename());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentDto> listDocuments(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(UserNotFoundException::new);

        List<Document> documents = documentRepository.findAll().stream()
                .filter(document -> canAccessDocument(user, document))
                .toList();

        return documents.stream().map(documentDtoMapper::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DocumentDto> listDocuments(String username, String filename, UUID departmentId, Pageable pageable) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        String normalizedFilename = filename == null || filename.isBlank() ? null : filename.trim();
        Page<DocumentDto> documents = documentRepository.listDocuments(
                        user.getId(), user.getRole(), normalizedFilename, departmentId, pageable)
                .map(documentDtoMapper::toDto);

        return PageResponse.from(documents);
    }

    @Override
    @Transactional
    public DocumentDto replaceDocument(UUID id, MultipartFile file, String username) throws IOException {
        if (file.isEmpty()) {
            throw new EmptyFileException();
        }

        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new DocumentNotFoundException(id));

        User user = userRepository.findByUsername(username)
                .orElseThrow(UserNotFoundException::new);

        if (!canReplaceDocument(user, document)) {
            throw new AccessDeniedException("Access denied");
        }

        ValidatedUpload validatedUpload = documentUploadValidationService.validate(file);
        String originalFilename = sanitizeFilename(file.getOriginalFilename());
        String storedFilename = UUID.randomUUID() + "_" + originalFilename;
        var parentPath = Paths.get(document.getFilepath()).getParent();
        if (parentPath == null) {
            throw new IllegalArgumentException("Invalid document path");
        }
        String directory = parentPath.toString();
        String filepath = fileStorageService.storeFile(directory, storedFilename, validatedUpload.content());

        fileStorageService.deleteFile(document.getFilepath());

        document.setFilename(originalFilename);
        document.setFilepath(filepath);
        document.setContentType(validatedUpload.detectedContentType());
        document.setSize((long) validatedUpload.content().length);

        Document saved = documentRepository.save(document);
        auditService.log("REPLACE", "Document", saved.getId().toString(), user.getId(),
                "Document replaced: " + originalFilename);
        log.info("Document replaced. documentId={}, userId={}", saved.getId(), user.getId());

        return documentDtoMapper.toDto(saved);
    }

    @Override
    @Transactional
    public void deleteDocument(UUID id, String username) throws IOException {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new DocumentNotFoundException(id));

        User user = userRepository.findByUsername(username)
                .orElseThrow(UserNotFoundException::new);

        if (!canDeleteDocument(user, document)) {
            throw new AccessDeniedException("Access denied");
        }

        fileStorageService.deleteFile(document.getFilepath());
        documentRepository.delete(document);
        auditService.log("DELETE", "Document", id.toString(), user.getId(),
                "Document deleted: " + document.getFilename());
        log.info("Document deleted. documentId={}, userId={}", id, user.getId());
    }

    private Department resolveUploadDepartment(User owner, UUID departmentId) {
        if (departmentId == null) {
            if (owner.getRole() == Role.MANAGER) {
                throw new SecurityException("Managers must upload files into departments they created");
            }
            return null;
        }

        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new DepartmentNotFoundException(departmentId.toString()));

        if (owner.getRole() == Role.ADMIN) {
            return department;
        }

        if (owner.getRole() == Role.MANAGER) {
            boolean isDepartmentManager = department.getManagers().stream()
                    .anyMatch(manager -> manager.getId().equals(owner.getId()));
            boolean belongsToDepartment = owner.getDepartments().stream()
                    .anyMatch(dept -> dept.getId().equals(department.getId()));
            if (!isDepartmentManager && !belongsToDepartment) {
                throw new AccessDeniedException("Access denied");
            }
            return department;
        }

        boolean isMember = owner.getDepartments().stream().anyMatch(d -> d.getId().equals(department.getId()));
        if (!isMember) {
            throw new SecurityException("Users can only upload to departments they belong to");
        }

        return department;
    }

    private boolean canAccessDocument(User user, Document document) {
        if (user.getRole() == Role.ADMIN) {
            return true;
        }

        if (document.getOwner().getId().equals(user.getId())) {
            return true;
        }

        if (user.getRole() == Role.USER) {
            return false;
        }

        Department department = document.getDepartment();
        if (department == null) {
            return document.getOwner().getId().equals(user.getId());
        }

        boolean isDepartmentManager = department.getManagers().stream()
                .anyMatch(manager -> manager.getId().equals(user.getId()));
        boolean belongsToDepartment = user.getDepartments().stream()
                .anyMatch(dept -> dept.getId().equals(department.getId()));

        return isDepartmentManager || belongsToDepartment;
    }

    private boolean canReplaceDocument(User user, Document document) {
        if (user.getRole() == Role.ADMIN) {
            return true;
        }

        if (document.getOwner().getId().equals(user.getId())) {
            return true;
        }

        if (user.getRole() != Role.MANAGER || document.getDepartment() == null) {
            return false;
        }

        return document.getDepartment().getManagers().stream()
                .anyMatch(manager -> manager.getId().equals(user.getId()));
    }

    private boolean canDeleteDocument(User user, Document document) {
        if (user.getRole() == Role.ADMIN) {
            return true;
        }
        if (document.getOwner().getId().equals(user.getId())) {
            return true;
        }
        return canAccessDocument(user, document);
    }

    private static final int MAX_FILENAME_LENGTH = 200;

    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "unnamed";
        }
        String name;
        try {
            Path fileName = Paths.get(filename).getFileName();
            if (fileName == null) {
                return "unnamed";
            }
            name = fileName.toString();
        } catch (InvalidPathException ex) {
            log.warn("Invalid filename input received; using fallback name.", ex);
            return "unnamed";
        }
        name = name.replaceAll("[^a-zA-Z0-9._\\-]", "_");
        if (name.length() > MAX_FILENAME_LENGTH) {
            name = name.substring(0, MAX_FILENAME_LENGTH);
        }
        return name.isBlank() ? "unnamed" : name;
    }
}
