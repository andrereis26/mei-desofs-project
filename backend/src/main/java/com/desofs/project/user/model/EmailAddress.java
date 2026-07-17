package com.desofs.project.user.model;

import com.desofs.project.shared.validation.BoundedText;

public record EmailAddress(String value) {

    private static final int MAX_LENGTH = 100;

    public EmailAddress {
        value = BoundedText.required(value, "Email", MAX_LENGTH);
        if (!hasValidFormat(value)) {
            throw new IllegalArgumentException("Email must be valid");
        }
    }

    public static EmailAddress of(String value) {
        return new EmailAddress(value);
    }

    private static boolean hasValidFormat(String value) {
        int atIndex = value.indexOf('@');
        if (atIndex <= 0 || atIndex != value.lastIndexOf('@') || atIndex == value.length() - 1) {
            return false;
        }

        if (value.chars().anyMatch(Character::isWhitespace)) {
            return false;
        }

        String domain = value.substring(atIndex + 1);
        int dotIndex = domain.indexOf('.');
        return dotIndex > 0 && dotIndex < domain.length() - 1;
    }
}
