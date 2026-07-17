package com.desofs.project.document.services;

import java.util.Arrays;
import java.util.Objects;

public record ValidatedUpload(byte[] content, String detectedContentType) {

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ValidatedUpload that)) {
            return false;
        }
        return Arrays.equals(content, that.content)
                && Objects.equals(detectedContentType, that.detectedContentType);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(content);
        result = 31 * result + Objects.hashCode(detectedContentType);
        return result;
    }

    @Override
    public String toString() {
        return "ValidatedUpload[content=" + Arrays.toString(content)
                + ", detectedContentType=" + detectedContentType
                + "]";
    }
}
