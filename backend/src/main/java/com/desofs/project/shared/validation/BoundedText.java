package com.desofs.project.shared.validation;

public final class BoundedText {

    private BoundedText() {
    }

    public static String required(String value, String fieldName, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return normalizePresent(value, fieldName, maxLength);
    }

    public static String optional(String value, String fieldName, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return normalizePresent(value, fieldName, maxLength);
    }

    private static String normalizePresent(String value, String fieldName, int maxLength) {
        String normalized = value.trim();
        validateLength(normalized, fieldName, maxLength);
        validateNoControlCharacters(normalized, fieldName);
        return normalized;
    }

    private static void validateLength(String value, String fieldName, int maxLength) {
        if (value.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " cannot exceed " + maxLength + " characters");
        }
    }

    private static void validateNoControlCharacters(String value, String fieldName) {
        if (value.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException(fieldName + " contains invalid characters");
        }
    }
}
