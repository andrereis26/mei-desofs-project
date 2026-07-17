package com.desofs.project.document.controller;

import com.desofs.project.config.JwtService;
import com.desofs.project.department.model.Department;
import com.desofs.project.department.repositories.DepartmentRepository;
import com.desofs.project.document.dtos.DocumentDto;
import com.desofs.project.document.model.Document;
import com.desofs.project.document.repositories.DocumentRepository;
import com.desofs.project.document.services.DocumentService;
import com.desofs.project.user.model.Role;
import com.desofs.project.user.model.User;
import com.desofs.project.user.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.file-upload.max-size=1024",
        "app.file-upload.allowed-content-types=application/pdf"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DocumentControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private JwtService jwtService;

    private User owner;
    private User viewer;
    @Autowired
    private DocumentService documentService;

    @BeforeEach
    void setup() {
        documentRepository.deleteAll();
        userRepository.deleteAll();
        departmentRepository.deleteAll();

        owner = userRepository.save(User.builder()
                .username("doc_owner")
                .email("doc_owner@example.com")
                .password("$2a$10$7sD4G4YDYv6J8r5EqP8GXul8mgxXp95LQn9rj7XOxQw4AiVC6YQ7G")
                .role(Role.USER)
                .build());

        viewer = userRepository.save(User.builder()
                .username("doc_viewer")
                .email("doc_viewer@example.com")
                .password("$2a$10$7sD4G4YDYv6J8r5EqP8GXul8mgxXp95LQn9rj7XOxQw4AiVC6YQ7G")
                .role(Role.USER)
                .build());
    }

    @Test
    void listDocuments_WithoutToken_Returns401() throws Exception {
        mockMvc.perform(get("/api/documents"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getDocument_AsNonOwner_Returns403AndSafeMessage() throws Exception {
        Document document = documentRepository.save(Document.builder()
                .filename("private.txt")
                .filepath("/tmp/desofs-test-storage/private.txt")
                .contentType("text/plain")
                .size(12L)
                .owner(owner)
                .build());

        String viewerToken = jwtService.generateToken(toUserDetails(viewer));

        mockMvc.perform(get("/api/documents/" + document.getId())
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("about:access-denied"))
                .andExpect(jsonPath("$.detail").value("Access denied"));
    }

    @Test
    void downloadDocument_AsNonOwner_Returns403() throws Exception {
        Document document = documentRepository.save(Document.builder()
                .filename("private.txt")
                .filepath("/tmp/desofs-test-storage/private.txt")
                .contentType("text/plain")
                .size(12L)
                .owner(owner)
                .build());

        String viewerToken = jwtService.generateToken(toUserDetails(viewer));

        mockMvc.perform(get("/api/documents/" + document.getId() + "/download")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void user_CannotReplaceOtherUsersDocument() throws Exception {
        Document document = saveDocument("owner-replace-target.pdf", owner);
        String viewerToken = jwtService.generateToken(toUserDetails(viewer));

        mockMvc.perform(multipart("/api/documents/" + document.getId() + "/replace")
                        .file(new MockMultipartFile("file", "replacement.pdf", "application/pdf", createPdfBytes(false)))
                        .header("Authorization", "Bearer " + viewerToken)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("about:access-denied"))
                .andExpect(jsonPath("$.detail").value("Access denied"));
    }

    @Test
    void user_CannotDeleteOtherUsersDocument() throws Exception {
        Document document = saveDocument("owner-delete-target.pdf", owner);
        String viewerToken = jwtService.generateToken(toUserDetails(viewer));

        mockMvc.perform(delete("/api/documents/" + document.getId())
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("about:access-denied"))
                .andExpect(jsonPath("$.detail").value("Access denied"));
    }

    @Test
    void downloadDocument_AsOwner_ReturnsFile() throws Exception {
        DocumentDto uploaded = documentService.upload(
                new MockMultipartFile("file", "private.pdf", "application/pdf", createPdfBytes(false)),
                owner.getUsername(),
                null
        );

        String ownerToken = jwtService.generateToken(toUserDetails(owner));

        mockMvc.perform(get("/api/documents/" + uploaded.getId() + "/download")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"private.pdf\""))
                .andExpect(content().bytes(createPdfBytes(false)));

        // cleanup
        documentService.deleteDocument(uploaded.getId(), owner.getUsername());
    }

        @Test
        void uploadDocument_WithNonPdfContent_ReturnsProblemDetail() throws Exception {
                String ownerToken = jwtService.generateToken(toUserDetails(owner));

                mockMvc.perform(multipart("/api/documents/upload")
                                                .file(new MockMultipartFile("file", "notes.txt", "text/plain", "plain text".getBytes()))
                                                .header("Authorization", "Bearer " + ownerToken))
                                .andExpect(status().isUnsupportedMediaType())
                                .andExpect(jsonPath("$.type").value("about:unsupported-document-type"))
                                .andExpect(jsonPath("$.detail").value("Only PDF files are allowed"));
        }

        @Test
        void uploadDocument_WithDangerousPdf_ReturnsProblemDetail() throws Exception {
                String ownerToken = jwtService.generateToken(toUserDetails(owner));

                mockMvc.perform(multipart("/api/documents/upload")
                                                .file(new MockMultipartFile("file", "dangerous.pdf", "application/pdf", createPdfBytes(true)))
                                                .header("Authorization", "Bearer " + ownerToken))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.type").value("about:unsafe-pdf"))
                                .andExpect(jsonPath("$.detail").value("Uploaded PDF failed security inspection"));
        }

    @Test
    void uploadDocument_WithEmptyFile_ReturnsProblemDetail() throws Exception {
        String ownerToken = jwtService.generateToken(toUserDetails(owner));

        mockMvc.perform(multipart("/api/documents/upload")
                        .file(new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]))
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("about:empty-file"))
                .andExpect(jsonPath("$.detail").value("Uploaded file is empty"));
    }

    @Test
    void replaceDocument_WithEmptyFile_ReturnsProblemDetail() throws Exception {
        Document document = saveDocument("replace-target.txt", owner);
        String ownerToken = jwtService.generateToken(toUserDetails(owner));

        mockMvc.perform(multipart("/api/documents/" + document.getId() + "/replace")
                        .file(new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]))
                        .header("Authorization", "Bearer " + ownerToken)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("about:empty-file"))
                .andExpect(jsonPath("$.detail").value("Uploaded file is empty"));
    }

    @Test
    void uploadDocument_WithTooLargeFile_ReturnsProblemDetail() throws Exception {
        String ownerToken = jwtService.generateToken(toUserDetails(owner));

        mockMvc.perform(multipart("/api/documents/upload")
                                                .file(new MockMultipartFile("file", "large.txt", "text/plain", new byte[1025]))
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.type").value("about:file-too-large"))
                .andExpect(jsonPath("$.detail").value("File exceeds the maximum allowed size"));
    }

    @Test
    void replaceDocument_WithTooLargeFile_ReturnsProblemDetail() throws Exception {
        Document document = saveDocument("large-replace-target.txt", owner);
        String ownerToken = jwtService.generateToken(toUserDetails(owner));

        mockMvc.perform(multipart("/api/documents/" + document.getId() + "/replace")
                                                .file(new MockMultipartFile("file", "large.txt", "text/plain", new byte[1025]))
                        .header("Authorization", "Bearer " + ownerToken)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.type").value("about:file-too-large"))
                .andExpect(jsonPath("$.detail").value("File exceeds the maximum allowed size"));
    }

    @Test
    void listDocuments_FiltersUnauthorizedDocuments() throws Exception {
        saveDocument("owner-only.txt", owner);
        Document viewerDocument = saveDocument("viewer-only.txt", viewer);

        String viewerToken = jwtService.generateToken(toUserDetails(viewer));

        mockMvc.perform(get("/api/documents")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(viewerDocument.getId().toString()));
    }

    @Test
    void listDocuments_ReturnsPaginatedVisibleDocuments() throws Exception {
        saveDocument("alpha.txt", viewer);
        saveDocument("beta.txt", viewer);
        saveDocument("owner-only.txt", owner);

        String viewerToken = jwtService.generateToken(toUserDetails(viewer));

        mockMvc.perform(get("/api/documents/directory")
                        .param("page", "0")
                        .param("size", "1")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.size", is(1)))
                .andExpect(jsonPath("$.totalElements", is(2)))
                .andExpect(jsonPath("$.totalPages", is(2)));
    }

    @Test
    void listDocuments_FiltersByFilename() throws Exception {
        Document report = saveDocument("finance-report.pdf", viewer);
        saveDocument("notes.txt", viewer);

        String viewerToken = jwtService.generateToken(toUserDetails(viewer));

        mockMvc.perform(get("/api/documents/directory")
                        .param("filename", "report")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(report.getId().toString()))
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.totalPages", is(1)));
    }

    @Test
    void listDocuments_FiltersByDepartment() throws Exception {
        Department finance = departmentRepository.save(Department.builder()
                .name("Finance")
                .description("Finance documents")
                .createdBy(viewer.getId())
                .build());
        Document budget = saveDocument("budget.pdf", viewer, finance);
        saveDocument("personal.txt", viewer);

        String viewerToken = jwtService.generateToken(toUserDetails(viewer));

        mockMvc.perform(get("/api/documents/directory")
                        .param("departmentId", finance.getId().toString())
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(budget.getId().toString()))
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.totalPages", is(1)));
    }

    @Test
    void listDocuments_AdminCanListAllDocuments() throws Exception {
        User admin = saveUser("doc_admin", Role.ADMIN);
        Document ownerDocument = saveDocument("owner-admin-visible.txt", owner);
        Document viewerDocument = saveDocument("viewer-admin-visible.txt", viewer);

        String adminToken = jwtService.generateToken(toUserDetails(admin));

        mockMvc.perform(get("/api/documents/directory")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[*].id", containsInAnyOrder(
                        ownerDocument.getId().toString(),
                        viewerDocument.getId().toString())))
                .andExpect(jsonPath("$.totalElements", is(2)))
                .andExpect(jsonPath("$.totalPages", is(1)));
    }

    @Test
    void listDocuments_ManagerCanListManagedAndMemberDepartmentDocuments() throws Exception {
        User manager = saveUser("doc_manager", Role.MANAGER);
        Department managedDepartment = saveDepartment("Managed Documents", manager, Set.of(manager));
        Department memberDepartment = saveDepartment("Member Documents", manager, Set.of());
        manager.getDepartments().add(memberDepartment);
        manager = userRepository.save(manager);

        Document managedDocument = saveDocument("managed-visible.txt", owner, managedDepartment);
        Document memberDocument = saveDocument("member-visible.txt", owner, memberDepartment);
        saveDocument("owner-personal.txt", owner);

        String managerToken = jwtService.generateToken(toUserDetails(manager));

        mockMvc.perform(get("/api/documents/directory")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[*].id", containsInAnyOrder(
                        managedDocument.getId().toString(),
                        memberDocument.getId().toString())))
                .andExpect(jsonPath("$.totalElements", is(2)))
                .andExpect(jsonPath("$.totalPages", is(1)));
    }

    @Test
    void listDocuments_ManagerCannotListUnrelatedDepartmentDocuments() throws Exception {
        User manager = saveUser("isolated_manager", Role.MANAGER);
        Department managedDepartment = saveDepartment("Accessible Documents", manager, Set.of(manager));
        Department unrelatedDepartment = saveDepartment("Unrelated Documents", owner, Set.of());

        Document visibleDocument = saveDocument("visible-managed.txt", owner, managedDepartment);
        saveDocument("hidden-unrelated.txt", owner, unrelatedDepartment);

        String managerToken = jwtService.generateToken(toUserDetails(manager));

        mockMvc.perform(get("/api/documents/directory")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(visibleDocument.getId().toString()))
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.totalPages", is(1)));
    }

    @Test
    void manager_CannotDownloadDocumentFromUnrelatedDepartment() throws Exception {
        User manager = saveUser("download_manager", Role.MANAGER);
        Department unrelatedDepartment = saveDepartment("Download Unrelated Documents", owner, Set.of());
        Document document = saveDocument("hidden-download.pdf", owner, unrelatedDepartment);

        String managerToken = jwtService.generateToken(toUserDetails(manager));

        mockMvc.perform(get("/api/documents/" + document.getId() + "/download")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("about:access-denied"))
                .andExpect(jsonPath("$.detail").value("Access denied"));
    }

    @Test
    void listDocuments_FiltersByFilenameAndDepartment() throws Exception {
        Department finance = saveDepartment("Finance Reports", viewer, Set.of());
        Department operations = saveDepartment("Operations Reports", viewer, Set.of());
        Document expected = saveDocument("budget-report.pdf", viewer, finance);
        saveDocument("budget-report.pdf", viewer, operations);
        saveDocument("finance-notes.txt", viewer, finance);

        String viewerToken = jwtService.generateToken(toUserDetails(viewer));

        mockMvc.perform(get("/api/documents/directory")
                        .param("filename", "budget")
                        .param("departmentId", finance.getId().toString())
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(expected.getId().toString()))
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.totalPages", is(1)));
    }

    @Test
    void listDocuments_CannotUseInvalidPaginationParameters() throws Exception {
        String viewerToken = jwtService.generateToken(toUserDetails(viewer));

        mockMvc.perform(get("/api/documents/directory")
                        .param("page", "-1")
                        .param("size", "20")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/documents/directory")
                        .param("page", "0")
                        .param("size", "101")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listDocuments_CannotUseInvalidDirectoryFilters() throws Exception {
        String viewerToken = jwtService.generateToken(toUserDetails(viewer));

        mockMvc.perform(get("/api/documents/directory")
                        .param("filename", "a".repeat(256))
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("about:validation-error"));

        mockMvc.perform(get("/api/documents/directory")
                        .param("departmentId", "not-a-uuid")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("about:validation-error"));
    }

    private User saveUser(String username, Role role) {
        return userRepository.save(User.builder()
                .username(username)
                .email(username + "@example.com")
                .password("$2a$10$7sD4G4YDYv6J8r5EqP8GXul8mgxXp95LQn9rj7XOxQw4AiVC6YQ7G")
                .role(role)
                .build());
    }

    private Department saveDepartment(String name, User createdBy, Set<User> managers) {
        return departmentRepository.save(Department.builder()
                .name(name)
                .description(name + " description")
                .managers(managers)
                .createdBy(createdBy.getId())
                .build());
    }

    private Document saveDocument(String filename, User owner) {
        return saveDocument(filename, owner, null);
    }

    private Document saveDocument(String filename, User owner, Department department) {
        return documentRepository.save(Document.builder()
                .filename(filename)
                .filepath("/tmp/desofs-test-storage/" + filename)
                .contentType("text/plain")
                .size(8L)
                .owner(owner)
                .department(department)
                .build());
    }

    private UserDetails toUserDetails(User user) {
        return org.springframework.security.core.userdetails.User.withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities("ROLE_" + user.getRole().name())
                .build();
    }

        private byte[] createPdfBytes(boolean withJavaScriptAction) {
                String catalog = "<< /Type /Catalog /Pages 2 0 R" + (withJavaScriptAction ? " /OpenAction 5 0 R" : "") + " >>";
                String pages = "<< /Type /Pages /Kids [3 0 R] /Count 1 >>";
                String page = "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 200 200] /Contents 4 0 R /Resources << >> >>";
                String stream = "<< /Length 0 >>\nstream\n\nendstream";
                String action = "<< /Type /Action /S /JavaScript /JS (app.alert('x')) >>";

                StringBuilder builder = new StringBuilder("%PDF-1.7\n");
                int objectCount = withJavaScriptAction ? 5 : 4;

                int[] offsets = new int[objectCount + 1];
                for (int index = 1; index <= objectCount; index++) {
                        offsets[index] = builder.toString().getBytes(StandardCharsets.US_ASCII).length;
                        builder.append(index).append(" 0 obj\n");
                        switch (index) {
                                case 1 -> builder.append(catalog);
                                case 2 -> builder.append(pages);
                                case 3 -> builder.append(page);
                                case 4 -> builder.append(stream);
                                case 5 -> builder.append(action);
                                default -> throw new IllegalArgumentException("Unexpected object index");
                        }
                        builder.append("\nendobj\n");
                }

                int xrefOffset = builder.toString().getBytes(StandardCharsets.US_ASCII).length;
                builder.append("xref\n");
                builder.append("0 ").append(objectCount + 1).append("\n");
                builder.append("0000000000 65535 f \n");
                for (int index = 1; index <= objectCount; index++) {
                        builder.append(String.format("%010d 00000 n \n", offsets[index]));
                }
                builder.append("trailer\n");
                builder.append("<< /Root 1 0 R /Size ").append(objectCount + 1).append(" >>\n");
                builder.append("startxref\n");
                builder.append(xrefOffset).append("\n");
                builder.append("%%EOF");

                return builder.toString().getBytes(StandardCharsets.US_ASCII);
        }
}
