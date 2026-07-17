package com.desofs.project.document.services;

import java.util.Arrays;
import java.util.Objects;

public record DocumentDownload(byte[] content, String contentType, String filename) {

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof DocumentDownload that)) {
            return false;
        }
        return Arrays.equals(content, that.content)
                && Objects.equals(contentType, that.contentType)
                && Objects.equals(filename, that.filename);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(content);
        result = 31 * result + Objects.hashCode(contentType);
        result = 31 * result + Objects.hashCode(filename);
        return result;
    }

    @Override
    public String toString() {
        return "DocumentDownload[content=" + Arrays.toString(content)
                + ", contentType=" + contentType
                + ", filename=" + filename
                + "]";
    }
}
