package com.desofs.project.document.services;

import com.desofs.project.audit.services.IAuditService;
import com.desofs.project.department.model.Department;
import com.desofs.project.department.repositories.DepartmentRepository;
import com.desofs.project.document.dtos.DocumentDto;
import com.desofs.project.document.mapper.DocumentDtoMapper;
import com.desofs.project.document.model.Document;
import com.desofs.project.document.repositories.DocumentRepository;
import com.desofs.project.document.services.DocumentUploadValidationService;
import com.desofs.project.document.services.ValidatedUpload;
import com.desofs.project.infrastructure.filesystem.FileStorageService;
import com.desofs.project.shared.dtos.PageResponse;
import com.desofs.project.shared.exceptions.EmptyFileException;
import com.desofs.project.user.model.Role;
import com.desofs.project.user.model.User;
import com.desofs.project.user.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private IAuditService auditService;

    @Mock
    private DocumentDtoMapper documentDtoMapper;

        @Mock
        private DocumentUploadValidationService documentUploadValidationService;

    @InjectMocks
    private DocumentService documentService;

    private User testUser;
    private Document testDocument;
    private UUID testUserId;
    private UUID testDocId;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(documentService, "storagePath", "./test-uploads");

        testUserId = UUID.randomUUID();
        testDocId = UUID.randomUUID();

        testUser = User.builder()
                .id(testUserId)
                .username("testuser")
                .email("test@example.com")
                .password("encodedPassword")
                .role(Role.USER)
                .build();

        testDocument = Document.builder()
                .id(testDocId)
                .filename("test.txt")
                .filepath("./test-uploads/" + testUserId + "/test.txt")
                .contentType("text/plain")
                .size(100L)
                .owner(testUser)
                .build();
    }

    @Test
    void upload_WhenValidFile_StoresAndReturnsDto() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", "test content".getBytes());

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(documentUploadValidationService.validate(file))
                .thenReturn(new ValidatedUpload("test content".getBytes(), "application/pdf"));
        when(fileStorageService.storeFile(anyString(), anyString(), any(byte[].class)))
                .thenReturn("./test-uploads/" + testUserId + "/test.txt");
        when(documentRepository.save(any(Document.class))).thenReturn(testDocument);
        when(documentDtoMapper.toDto(testDocument)).thenReturn(DocumentDto.builder().id(testDocId).filename("test.txt").ownerId(testUserId).build());
        when(auditService.log(anyString(), anyString(), anyString(), any(), anyString())).thenReturn(null);

        DocumentDto result = documentService.upload(file, "testuser", null);

        assertThat(result).isNotNull();
        assertThat(result.getFilename()).isEqualTo("test.txt");
        verify(fileStorageService).storeFile(anyString(), anyString(), any(byte[].class));
        verify(documentRepository).save(any(Document.class));
    }

    @Test
    void upload_WhenFileIsEmpty_ThrowsEmptyFileException() {
        MockMultipartFile file = new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]);

        assertThatThrownBy(() -> documentService.upload(file, "testuser", null))
                .isInstanceOf(EmptyFileException.class);
        verifyNoInteractions(userRepository, documentRepository, fileStorageService);
    }

    @Test
    void upload_WhenManagerHasNoDepartment_RejectsUpload() {
        testUser.setRole(Role.MANAGER);
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "data".getBytes());
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> documentService.upload(file, "testuser", null))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Managers must upload");
    }

    @Test
    void upload_WhenUserDoesNotBelongToDepartment_RejectsUpload() {
        UUID departmentId = UUID.randomUUID();
        Department department = Department.builder().id(departmentId).name("Engineering").build();
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "data".getBytes());

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(department));

        assertThatThrownBy(() -> documentService.upload(file, "testuser", departmentId))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Users can only upload");
    }

    @Test
    void upload_WhenAdminTargetsDepartment_StoresDepartmentDocument() throws IOException {
        testUser.setRole(Role.ADMIN);
        UUID departmentId = UUID.randomUUID();
        Department department = Department.builder().id(departmentId).name("Engineering").build();
        MockMultipartFile file = new MockMultipartFile("file", "admin report.pdf", "application/pdf", "data".getBytes());

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(department));
        when(documentUploadValidationService.validate(file))
                .thenReturn(new ValidatedUpload("data".getBytes(), "application/pdf"));
        when(fileStorageService.storeFile(anyString(), anyString(), any(byte[].class)))
                .thenReturn("./test-uploads/" + testUserId + "/admin_report.pdf");
        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> {
            Document document = invocation.getArgument(0);
            document.setId(testDocId);
            return document;
        });
        when(documentDtoMapper.toDto(any(Document.class))).thenReturn(DocumentDto.builder()
                .id(testDocId)
                .filename("admin_report.pdf")
                .ownerId(testUserId)
                .departmentId(departmentId)
                .build());

        DocumentDto result = documentService.upload(file, "testuser", departmentId);

        assertThat(result.getDepartmentId()).isEqualTo(departmentId);
        verify(departmentRepository).findById(departmentId);
        verify(fileStorageService).createStorageDirectory("./test-uploads/" + testUserId);
    }

    @Test
    void upload_WhenManagerManagesDepartment_StoresDepartmentDocument() throws IOException {
        testUser.setRole(Role.MANAGER);
        UUID departmentId = UUID.randomUUID();
        Department department = Department.builder()
                .id(departmentId)
                .name("Engineering")
                .managers(Set.of(testUser))
                .build();
        MockMultipartFile file = new MockMultipartFile("file", "manager report.txt", "text/plain", "data".getBytes());

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(department));
        when(documentUploadValidationService.validate(file))
                .thenReturn(new ValidatedUpload("data".getBytes(), "application/pdf"));
        when(fileStorageService.storeFile(anyString(), anyString(), any(byte[].class)))
                .thenReturn("./test-uploads/" + testUserId + "/manager_report.txt");
        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> {
            Document document = invocation.getArgument(0);
            document.setId(testDocId);
            return document;
        });
        when(documentDtoMapper.toDto(any(Document.class))).thenReturn(DocumentDto.builder()
                .id(testDocId)
                .filename("manager_report.txt")
                .ownerId(testUserId)
                .departmentId(departmentId)
                .build());

        DocumentDto result = documentService.upload(file, "testuser", departmentId);

        assertThat(result.getDepartmentId()).isEqualTo(departmentId);
        verify(auditService).log(eq("UPLOAD"), eq("Document"), anyString(), eq(testUserId),
                contains("manager_report.txt"));
    }

    @Test
    void upload_WhenManagerBelongsToDepartment_StoresDepartmentDocument() throws IOException {
        testUser.setRole(Role.MANAGER);
        UUID departmentId = UUID.randomUUID();
        Department department = Department.builder().id(departmentId).name("Engineering").build();
        testUser.setDepartments(Set.of(department));
        MockMultipartFile file = new MockMultipartFile("file", "member report.txt", "text/plain", "data".getBytes());

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(department));
        when(documentUploadValidationService.validate(file))
                .thenReturn(new ValidatedUpload("data".getBytes(), "application/pdf"));
        when(fileStorageService.storeFile(anyString(), anyString(), any(byte[].class)))
                .thenReturn("./test-uploads/" + testUserId + "/member_report.txt");
        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> {
            Document document = invocation.getArgument(0);
            document.setId(testDocId);
            return document;
        });
        when(documentDtoMapper.toDto(any(Document.class))).thenReturn(DocumentDto.builder()
                .id(testDocId)
                .filename("member_report.txt")
                .ownerId(testUserId)
                .departmentId(departmentId)
                .build());

        DocumentDto result = documentService.upload(file, "testuser", departmentId);

        assertThat(result.getFilename()).isEqualTo("member_report.txt");
    }

    @Test
    void getDocument_WhenOwnerRequestsDocument_ReturnsDto() {
        DocumentDto dto = DocumentDto.builder().id(testDocId).filename("test.txt").ownerId(testUserId).build();
        when(documentRepository.findById(testDocId)).thenReturn(Optional.of(testDocument));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(documentDtoMapper.toDto(testDocument)).thenReturn(dto);

        assertThat(documentService.getDocument(testDocId, "testuser")).isEqualTo(dto);
    }

    @Test
    void getDocument_WhenUnrelatedUserRequestsDocument_ThrowsAccessDenied() {
        User otherUser = User.builder()
                .id(UUID.randomUUID())
                .username("other")
                .role(Role.USER)
                .build();
        when(documentRepository.findById(testDocId)).thenReturn(Optional.of(testDocument));
        when(userRepository.findByUsername("other")).thenReturn(Optional.of(otherUser));

        assertThatThrownBy(() -> documentService.getDocument(testDocId, "other"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void downloadDocument_WhenDepartmentManagerRequestsDocument_ReadsFileAndAudits() throws IOException {
        User manager = managerUser();
        Department department = Department.builder()
                .id(UUID.randomUUID())
                .name("Engineering")
                .managers(Set.of(manager))
                .build();
        testDocument.setDepartment(department);
        when(documentRepository.findById(testDocId)).thenReturn(Optional.of(testDocument));
        when(userRepository.findByUsername("manager")).thenReturn(Optional.of(manager));
        when(fileStorageService.readFile(testDocument.getFilepath())).thenReturn("content".getBytes());

        DocumentDownload result = documentService.downloadDocument(testDocId, "manager");

        assertThat(result.content()).isEqualTo("content".getBytes());
        assertThat(result.contentType()).isEqualTo("text/plain");
        assertThat(result.filename()).isEqualTo("test.txt");
        verify(auditService).log(eq("DOWNLOAD"), eq("Document"), eq(testDocId.toString()), eq(manager.getId()),
                contains("test.txt"));
    }

    @Test
    void listDocuments_ReturnsOnlyDocumentsVisibleToUser() {
        User otherOwner = User.builder().id(UUID.randomUUID()).username("other").role(Role.USER).build();
        Document otherDocument = Document.builder()
                .id(UUID.randomUUID())
                .filename("other.txt")
                .filepath("./test-uploads/other/other.txt")
                .contentType("text/plain")
                .size(10L)
                .owner(otherOwner)
                .build();
        DocumentDto dto = DocumentDto.builder().id(testDocId).filename("test.txt").ownerId(testUserId).build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(documentRepository.findAll()).thenReturn(List.of(testDocument, otherDocument));
        when(documentDtoMapper.toDto(testDocument)).thenReturn(dto);

        assertThat(documentService.listDocuments("testuser")).containsExactly(dto);
    }

    @Test
    void replaceDocument_WhenOwnerReplacesDocument_StoresNewFileDeletesOldAndAudits() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "new report.txt", "text/plain", "new".getBytes());
        when(documentRepository.findById(testDocId)).thenReturn(Optional.of(testDocument));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(documentUploadValidationService.validate(file))
                .thenReturn(new ValidatedUpload("new".getBytes(), "application/pdf"));
        when(fileStorageService.storeFile(anyString(), anyString(), any(byte[].class)))
                .thenReturn("./test-uploads/" + testUserId + "/new_report.txt");
        when(documentRepository.save(testDocument)).thenReturn(testDocument);
        when(documentDtoMapper.toDto(testDocument)).thenReturn(DocumentDto.builder()
                .id(testDocId)
                .filename("new_report.txt")
                .ownerId(testUserId)
                .build());

        DocumentDto result = documentService.replaceDocument(testDocId, file, "testuser");

        assertThat(result.getFilename()).isEqualTo("new_report.txt");
        verify(fileStorageService).deleteFile("./test-uploads/" + testUserId + "/test.txt");
        verify(auditService).log(eq("REPLACE"), eq("Document"), eq(testDocId.toString()), eq(testUserId),
                contains("new_report.txt"));
    }

    @Test
    void replaceDocument_WhenStoredPathHasNoParent_RejectsDocumentPath() {
        testDocument.setFilepath("orphan.txt");
        MockMultipartFile file = new MockMultipartFile("file", "new.txt", "text/plain", "new".getBytes());
        when(documentRepository.findById(testDocId)).thenReturn(Optional.of(testDocument));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> documentService.replaceDocument(testDocId, file, "testuser"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid document path");
    }

    @Test
    void replaceDocument_WhenDepartmentManagerReplacesDocument_StoresNewFile() throws IOException {
        User manager = managerUser();
        Department department = Department.builder()
                .id(UUID.randomUUID())
                .name("Engineering")
                .managers(Set.of(manager))
                .build();
        testDocument.setDepartment(department);
        MockMultipartFile file = new MockMultipartFile("file", "team report.txt", "text/plain", "team".getBytes());

        when(documentRepository.findById(testDocId)).thenReturn(Optional.of(testDocument));
        when(userRepository.findByUsername("manager")).thenReturn(Optional.of(manager));
        when(documentUploadValidationService.validate(file))
                .thenReturn(new ValidatedUpload("team".getBytes(), "application/pdf"));
        when(fileStorageService.storeFile(anyString(), anyString(), any(byte[].class)))
                .thenReturn("./test-uploads/" + testUserId + "/team_report.txt");
        when(documentRepository.save(testDocument)).thenReturn(testDocument);
        when(documentDtoMapper.toDto(testDocument)).thenReturn(DocumentDto.builder()
                .id(testDocId)
                .filename("team_report.txt")
                .ownerId(testUserId)
                .build());

        DocumentDto result = documentService.replaceDocument(testDocId, file, "manager");

        assertThat(result.getFilename()).isEqualTo("team_report.txt");
        verify(fileStorageService).deleteFile("./test-uploads/" + testUserId + "/test.txt");
        verify(auditService).log(eq("REPLACE"), eq("Document"), eq(testDocId.toString()), eq(manager.getId()),
                contains("team_report.txt"));
    }

    @Test
    void replaceDocument_WhenDepartmentMemberIsNotManager_RejectsReplace() {
        Department department = Department.builder().id(UUID.randomUUID()).name("Engineering").build();
        User member = User.builder()
                .id(UUID.randomUUID())
                .username("member")
                .role(Role.MANAGER)
                .departments(Set.of(department))
                .build();
        testDocument.setDepartment(department);
        MockMultipartFile file = new MockMultipartFile("file", "new.txt", "text/plain", "new".getBytes());

        when(documentRepository.findById(testDocId)).thenReturn(Optional.of(testDocument));
        when(userRepository.findByUsername("member")).thenReturn(Optional.of(member));

        assertThatThrownBy(() -> documentService.replaceDocument(testDocId, file, "member"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void deleteDocument_WhenAdminDeletesDocument_DeletesFileDocumentAndAudits() throws IOException {
        User admin = User.builder().id(UUID.randomUUID()).username("admin").role(Role.ADMIN).build();
        when(documentRepository.findById(testDocId)).thenReturn(Optional.of(testDocument));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        documentService.deleteDocument(testDocId, "admin");

        verify(fileStorageService).deleteFile(testDocument.getFilepath());
        verify(documentRepository).delete(testDocument);
        verify(auditService).log(eq("DELETE"), eq("Document"), eq(testDocId.toString()), eq(admin.getId()),
                contains("test.txt"));
    }

    @Test
    void deleteDocument_WhenDepartmentMemberDeletesDocument_UsesAccessRules() throws IOException {
        Department department = Department.builder().id(UUID.randomUUID()).name("Engineering").build();
        User member = User.builder()
                .id(UUID.randomUUID())
                .username("member")
                .role(Role.MANAGER)
                .departments(Set.of(department))
                .build();
        testDocument.setDepartment(department);
        when(documentRepository.findById(testDocId)).thenReturn(Optional.of(testDocument));
        when(userRepository.findByUsername("member")).thenReturn(Optional.of(member));

        documentService.deleteDocument(testDocId, "member");

        verify(fileStorageService).deleteFile(testDocument.getFilepath());
        verify(documentRepository).delete(testDocument);
        verify(auditService).log(eq("DELETE"), eq("Document"), eq(testDocId.toString()), eq(member.getId()),
                contains("test.txt"));
    }

    @Test
    void listDocuments_WhenVisibleDocuments_ReturnsPagedResponse() {
        Pageable pageable = PageRequest.of(0, 1);
        DocumentDto dto = DocumentDto.builder()
                .id(testDocId)
                .filename("test.txt")
                .ownerId(testUserId)
                .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(documentRepository.listDocuments(testUserId, Role.USER, "test", null, pageable))
                .thenReturn(new PageImpl<>(List.of(testDocument), pageable, 2));
        when(documentDtoMapper.toDto(testDocument)).thenReturn(dto);

        PageResponse<DocumentDto> result = documentService.listDocuments("testuser", " test ", null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getFilename()).isEqualTo("test.txt");
        assertThat(result.getPage()).isZero();
        assertThat(result.getSize()).isEqualTo(1);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getTotalPages()).isEqualTo(2);
        verify(documentRepository).listDocuments(testUserId, Role.USER, "test", null, pageable);
    }

    @Test
    void listDocuments_WhenNoDocuments_ReturnsEmptyPagedResponse() {
        Pageable pageable = PageRequest.of(0, 10);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(documentRepository.listDocuments(testUserId, Role.USER, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        PageResponse<DocumentDto> result = documentService.listDocuments("testuser", " ", null, pageable);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getPage()).isZero();
        assertThat(result.getSize()).isEqualTo(10);
        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getTotalPages()).isZero();
        verify(documentRepository).listDocuments(testUserId, Role.USER, null, null, pageable);
    }

    private User managerUser() {
        return User.builder()
                .id(UUID.randomUUID())
                .username("manager")
                .email("manager@example.com")
                .role(Role.MANAGER)
                .build();
    }
}
