package com.desofs.project.document.services;

import com.desofs.project.document.dtos.DocumentDto;
import com.desofs.project.shared.dtos.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public interface IDocumentService {
    DocumentDto upload(MultipartFile file, String username, UUID departmentId) throws IOException;
    DocumentDto getDocument(UUID id, String username);
    DocumentDownload downloadDocument(UUID id, String username) throws IOException;
    List<DocumentDto> listDocuments(String username);
    PageResponse<DocumentDto> listDocuments(String username, String filename, UUID departmentId, Pageable pageable);
    DocumentDto replaceDocument(UUID id, MultipartFile file, String username) throws IOException;
    void deleteDocument(UUID id, String username) throws IOException;
}
