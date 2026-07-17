package com.desofs.project.user.model;

import com.desofs.project.shared.validation.BoundedText;

public record Username(String value) {

    private static final int MIN_LENGTH = 3;
    private static final int MAX_LENGTH = 50;

    public Username {
        value = BoundedText.required(value, "Username", MAX_LENGTH);
        if (value.length() < MIN_LENGTH) {
            throw new IllegalArgumentException("Username must be between 3 and 50 characters");
        }
    }

    public static Username of(String value) {
        return new Username(value);
    }
}
