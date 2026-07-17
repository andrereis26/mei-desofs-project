package com.desofs.project.shared.persistence;

import java.util.Locale;

public final class LikePatternEscaper {

    public static final char ESCAPE_CHARACTER = '!';

    private LikePatternEscaper() {
    }

    public static String containsIgnoreCase(String value) {
        return "%" + escape(value.toLowerCase(Locale.ROOT)) + "%";
    }

    public static String escape(String value) {
        StringBuilder escaped = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (current == ESCAPE_CHARACTER || current == '%' || current == '_') {
                escaped.append(ESCAPE_CHARACTER);
            }
            escaped.append(current);
        }
        return escaped.toString();
    }
}
