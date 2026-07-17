package com.desofs.project.document.model;

import com.desofs.project.shared.validation.BoundedText;

public record DocumentMetadata(String filename, String filepath, String contentType, long size) {

    private static final int MAX_FILENAME_LENGTH = 255;
    private static final int MAX_FILEPATH_LENGTH = 500;
    private static final int MAX_CONTENT_TYPE_LENGTH = 100;

    public DocumentMetadata {
        filename = BoundedText.required(filename, "Document filename", MAX_FILENAME_LENGTH);
        filepath = BoundedText.required(filepath, "Document filepath", MAX_FILEPATH_LENGTH);
        contentType = BoundedText.optional(contentType, "Document content type", MAX_CONTENT_TYPE_LENGTH);

        if (filename.contains("/") || filename.contains("\\") || ".".equals(filename) || "..".equals(filename)) {
            throw new IllegalArgumentException("Document filename cannot contain path segments");
        }
        if (size < 0) {
            throw new IllegalArgumentException("Document size cannot be negative");
        }
    }

    public static DocumentMetadata of(String filename, String filepath, String contentType, Long size) {
        if (size == null) {
            throw new IllegalArgumentException("Document size is required");
        }
        return new DocumentMetadata(filename, filepath, contentType, size);
    }
}
