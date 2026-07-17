package com.desofs.project.audit.model;

import com.desofs.project.shared.validation.BoundedText;

enum AuditTextField {
    ACTION("Audit action", 100),
    ENTITY_TYPE("Audit entity type", 100),
    ENTITY_ID("Audit target id", 255),
    DETAILS("Audit details", 2_000);

    private final String label;
    private final int maxLength;

    AuditTextField(String label, int maxLength) {
        this.label = label;
        this.maxLength = maxLength;
    }

    String required(String value) {
        return BoundedText.required(value, label, maxLength);
    }

    String optional(String value) {
        return BoundedText.optional(value, label, maxLength);
    }
}
