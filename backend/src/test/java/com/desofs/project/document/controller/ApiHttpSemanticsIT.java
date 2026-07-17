package com.desofs.project.document.controller;

import com.desofs.project.config.JwtService;
import com.desofs.project.department.repositories.DepartmentRepository;
import com.desofs.project.document.dtos.DocumentDto;
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
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.file-upload.max-size=1024",
        "app.file-upload.allowed-content-types=application/pdf"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiHttpSemanticsIT {

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

    @Autowired
    private DocumentService documentService;

    private User owner;

    @BeforeEach
    void setup() {
        documentRepository.deleteAll();
        departmentRepository.deleteAll();
        userRepository.deleteAll();

        owner = userRepository.save(User.builder()
                .username("http_semantics_owner")
                .email("http_semantics_owner@example.com")
                .password("$2a$10$7sD4G4YDYv6J8r5EqP8GXul8mgxXp95LQn9rj7XOxQw4AiVC6YQ7G")
                .role(Role.USER)
                .build());
    }

    @Test
    void jsonResponses_DeclareJsonContentType() throws Exception {
        mockMvc.perform(get("/api/documents")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    void problemResponses_DeclareProblemJsonContentType() throws Exception {
        mockMvc.perform(get("/api/documents/" + UUID.randomUUID())
                        .header("Authorization", bearerToken()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void fileDownloads_DeclareStoredContentType() throws Exception {
        DocumentDto uploaded = documentService.upload(
                new MockMultipartFile("file", "private.pdf", "application/pdf", createPdfBytes(false)),
                owner.getUsername(),
                null
        );

        mockMvc.perform(get("/api/documents/" + uploaded.getId() + "/download")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"private.pdf\""));

        documentService.deleteDocument(uploaded.getId(), owner.getUsername());
    }

    @Test
    void unsupportedHttpMethods_AreBlocked() throws Exception {
        mockMvc.perform(post("/api/documents")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isMethodNotAllowed());

        mockMvc.perform(request(HttpMethod.PATCH, "/api/documents")
                        .header("Authorization", bearerToken()))
                .andExpect(status().is4xxClientError());

        mockMvc.perform(request(HttpMethod.TRACE, "/api/documents")
                        .header("Authorization", bearerToken()))
                .andExpect(status().is4xxClientError());
    }

    private String bearerToken() {
        return "Bearer " + jwtService.generateToken(toUserDetails(owner));
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
