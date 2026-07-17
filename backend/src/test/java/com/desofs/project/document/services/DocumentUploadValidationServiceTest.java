package com.desofs.project.document.services;

import com.desofs.project.document.config.DocumentUploadProperties;
import com.desofs.project.shared.exceptions.UnsafePdfException;
import com.desofs.project.shared.exceptions.UnsupportedDocumentTypeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentUploadValidationServiceTest {

    private DocumentUploadValidationService validationService;

    @BeforeEach
    void setUp() {
        DocumentUploadProperties properties = new DocumentUploadProperties();
        properties.setAllowedContentTypes(List.of("application/pdf"));
        validationService = new DocumentUploadValidationService(properties);
    }

    @Test
    void validate_AcceptsSafePdf() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "safe.pdf", "application/pdf", createPdfBytes(false));

        ValidatedUpload validatedUpload = validationService.validate(file);

        assertThat(validatedUpload.detectedContentType()).isEqualTo("application/pdf");
        assertThat(validatedUpload.content()).isNotEmpty();
    }

    @Test
    void validate_RejectsNonPdfContent() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "notes.txt", "text/plain", "plain text".getBytes());

        assertThatThrownBy(() -> validationService.validate(file))
                .isInstanceOf(UnsupportedDocumentTypeException.class)
                .hasMessage("Only PDF files are allowed");
    }

    @Test
    void validate_RejectsPdfWithJavaScriptAction() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "dangerous.pdf", "application/pdf", createPdfBytes(true));

        assertThatThrownBy(() -> validationService.validate(file))
                .isInstanceOf(UnsafePdfException.class)
                .hasMessage("Uploaded PDF failed security inspection");
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
