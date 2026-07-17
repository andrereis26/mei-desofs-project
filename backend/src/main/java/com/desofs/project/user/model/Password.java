package com.desofs.project.user.model;

import com.desofs.project.shared.validation.BoundedText;

public record Password(String value) {

    private static final int MAX_LENGTH = 255;

    public Password {
        value = BoundedText.required(value, "Password", MAX_LENGTH);
    }

    public static Password of(String value) {
        return new Password(value);
    }
}
