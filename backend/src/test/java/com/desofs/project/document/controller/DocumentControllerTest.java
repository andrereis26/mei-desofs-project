package com.desofs.project.document.controller;

import com.desofs.project.document.dtos.DocumentDto;
import com.desofs.project.document.services.DocumentDownload;
import com.desofs.project.document.services.IDocumentService;
import com.desofs.project.shared.dtos.PageResponse;
import com.desofs.project.shared.exceptions.EmptyFileException;
import com.desofs.project.shared.exceptions.FileTooLargeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentControllerTest {

    @Mock
    private IDocumentService documentService;

    @InjectMocks
    private DocumentController controller;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(controller, "maxFileSize", 5L);
    }

    @Test
    void uploadDocument_ValidatesSizeAndDelegatesToService() throws IOException {
        UUID departmentId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "a.txt", "text/plain", "data".getBytes());
        DocumentDto dto = DocumentDto.builder().id(UUID.randomUUID()).filename("a.txt").build();
        when(documentService.upload(file, "alice", departmentId)).thenReturn(dto);

        var response = controller.uploadDocument(file, departmentId, principal("alice"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(dto);
        assertThatThrownBy(() -> controller.uploadDocument(
                new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]), null, principal("alice")))
                .isInstanceOf(EmptyFileException.class);
        assertThatThrownBy(() -> controller.uploadDocument(
                new MockMultipartFile("file", "large.txt", "text/plain", "too-large".getBytes()), null, principal("alice")))
                .isInstanceOf(FileTooLargeException.class);
    }

    @Test
    void readAndListEndpoints_DelegateToService() throws IOException {
        UUID id = UUID.randomUUID();
        UUID departmentId = UUID.randomUUID();
        DocumentDto dto = DocumentDto.builder().id(id).filename("a.txt").build();
        PageResponse<DocumentDto> page = PageResponse.<DocumentDto>builder().content(List.of(dto)).build();
        when(documentService.getDocument(id, "alice")).thenReturn(dto);
        when(documentService.downloadDocument(id, "alice"))
                .thenReturn(new DocumentDownload("data".getBytes(), "text/plain", "report\r\nX-Injected: yes.pdf"));
        when(documentService.listDocuments("alice")).thenReturn(List.of(dto));
        when(documentService.listDocuments(eq("alice"), eq("a"), eq(departmentId), any(Pageable.class))).thenReturn(page);

        assertThat(controller.getDocument(id, principal("alice")).getBody()).isEqualTo(dto);
        assertThat(controller.downloadDocument(id, principal("alice")).getBody()).isEqualTo("data".getBytes());
        assertThat(controller.downloadDocument(id, principal("alice")).getHeaders().getContentType())
                .isEqualTo(new MediaType(MediaType.TEXT_PLAIN, StandardCharsets.UTF_8));
        assertThat(controller.downloadDocument(id, principal("alice")).getHeaders().getFirst("Content-Disposition"))
                .contains("report__X-Injected__yes.pdf")
                .doesNotContain("\r", "\n", "X-Injected:");
        assertThat(controller.listDocuments(principal("alice")).getBody()).containsExactly(dto);
        assertThat(controller.listDocuments("a", departmentId, 0, 10, principal("alice")).getBody()).isEqualTo(page);
    }

    @Test
    void deleteAndReplace_DelegateToServiceAfterValidation() throws IOException {
        UUID id = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "a.txt", "text/plain", "data".getBytes());
        DocumentDto dto = DocumentDto.builder().id(id).filename("a.txt").build();
        when(documentService.replaceDocument(id, file, "alice")).thenReturn(dto);

        assertThat(controller.deleteDocument(id, principal("alice")).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(documentService).deleteDocument(id, "alice");
        assertThat(controller.replaceDocument(id, file, principal("alice")).getBody()).isEqualTo(dto);
        assertThatThrownBy(() -> controller.replaceDocument(
                id, new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]), principal("alice")))
                .isInstanceOf(EmptyFileException.class);
        assertThatThrownBy(() -> controller.replaceDocument(
                id, new MockMultipartFile("file", "large.txt", "text/plain", "too-large".getBytes()), principal("alice")))
                .isInstanceOf(FileTooLargeException.class);
    }

    private static UserDetails principal(String username) {
        return org.springframework.security.core.userdetails.User
                .withUsername(username)
                .password("encoded")
                .authorities("ROLE_USER")
                .build();
    }
}
