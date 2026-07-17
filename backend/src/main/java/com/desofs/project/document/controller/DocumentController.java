package com.desofs.project.document.controller;

import com.desofs.project.document.dtos.DocumentDto;
import com.desofs.project.document.services.DocumentDownload;
import com.desofs.project.document.services.IDocumentService;
import com.desofs.project.shared.dtos.PageResponse;
import com.desofs.project.shared.exceptions.EmptyFileException;
import com.desofs.project.shared.exceptions.FileTooLargeException;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Validated
public class DocumentController {

    private static final int MAX_DOWNLOAD_FILENAME_LENGTH = 200;

    private final IDocumentService documentService;

    @Value("${app.file-upload.max-size:10485760}")
    private long maxFileSize;

    @PostMapping("/upload")
    public ResponseEntity<DocumentDto> uploadDocument(@RequestParam("file") MultipartFile file,
                                                       @RequestParam(value = "departmentId", required = false) UUID departmentId,
                                                       @AuthenticationPrincipal UserDetails userDetails) throws IOException {
        if (file.isEmpty()) {
            throw new EmptyFileException();
        }
        if (file.getSize() > maxFileSize) {
            throw new FileTooLargeException();
        }
        DocumentDto dto = documentService.upload(file, userDetails.getUsername(), departmentId);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentDto> getDocument(@PathVariable UUID id,
                                                    @AuthenticationPrincipal UserDetails userDetails) {
        DocumentDto dto = documentService.getDocument(id, userDetails.getUsername());
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> downloadDocument(@PathVariable UUID id,
                                                   @AuthenticationPrincipal UserDetails userDetails) throws IOException {
        DocumentDownload download = documentService.downloadDocument(id, userDetails.getUsername());
        return ResponseEntity.ok()
                .contentType(resolveContentType(download.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition(download.filename(), id))
                .body(download.content());
    }

    @GetMapping
    public ResponseEntity<List<DocumentDto>> listDocuments(@AuthenticationPrincipal UserDetails userDetails) {
        List<DocumentDto> documents = documentService.listDocuments(userDetails.getUsername());
        return ResponseEntity.ok(documents);
    }

    @GetMapping("/directory")
    public ResponseEntity<PageResponse<DocumentDto>> listDocuments(
            @RequestParam(required = false) @Size(max = 255, message = "filename must be less than or equal to 255 characters") String filename,
            @RequestParam(required = false) UUID departmentId,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page must be greater than or equal to 0") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "size must be greater than or equal to 1")
            @Max(value = 100, message = "size must be less than or equal to 100") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        Pageable pageable = PageRequest.of(page, size);
        PageResponse<DocumentDto> documents = documentService.listDocuments(
                userDetails.getUsername(), filename, departmentId, pageable);
        return ResponseEntity.ok(documents);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(@PathVariable UUID id,
                                                 @AuthenticationPrincipal UserDetails userDetails) throws IOException {
        documentService.deleteDocument(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/replace")
    public ResponseEntity<DocumentDto> replaceDocument(@PathVariable UUID id,
                                                       @RequestParam("file") MultipartFile file,
                                                       @AuthenticationPrincipal UserDetails userDetails) throws IOException {
        if (file.isEmpty()) {
            throw new EmptyFileException();
        }
        if (file.getSize() > maxFileSize) {
            throw new FileTooLargeException();
        }
        return ResponseEntity.ok(documentService.replaceDocument(id, file, userDetails.getUsername()));
    }

    private MediaType resolveContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        try {
            MediaType mediaType = MediaType.parseMediaType(contentType);
            if (requiresCharset(mediaType) && mediaType.getCharset() == null) {
                return new MediaType(mediaType, StandardCharsets.UTF_8);
            }
            return mediaType;
        } catch (InvalidMediaTypeException ex) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    private boolean requiresCharset(MediaType mediaType) {
        return "text".equalsIgnoreCase(mediaType.getType())
                || mediaType.getSubtype().equalsIgnoreCase("xml")
                || mediaType.getSubtype().toLowerCase(Locale.ROOT).endsWith("+xml");
    }

    private String contentDisposition(String filename, UUID fallbackId) {
        return ContentDisposition.attachment()
                .filename(safeDownloadFilename(filename, fallbackId))
                .build()
                .toString();
    }

    private String safeDownloadFilename(String filename, UUID fallbackId) {
        if (filename == null || filename.isBlank()) {
            return fallbackId.toString();
        }

        String cleaned = filename.trim().replaceAll("[^a-zA-Z0-9._\\-]", "_");
        if (cleaned.length() > MAX_DOWNLOAD_FILENAME_LENGTH) {
            cleaned = cleaned.substring(0, MAX_DOWNLOAD_FILENAME_LENGTH);
        }
        return cleaned.isBlank() ? fallbackId.toString() : cleaned;
    }
}
