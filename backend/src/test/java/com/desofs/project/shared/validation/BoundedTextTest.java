package com.desofs.project.shared.validation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BoundedTextTest {

    @Test
    void required_TrimsPresentValue() {
        assertThat(BoundedText.required("  report.pdf  ", "Filename", 20)).isEqualTo("report.pdf");
    }

    @Test
    void required_RejectsMissingValue() {
        assertThatThrownBy(() -> BoundedText.required(null, "Filename", 20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Filename is required");

        assertThatThrownBy(() -> BoundedText.required(" ", "Filename", 20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Filename is required");
    }

    @Test
    void optional_ReturnsNullForMissingValueAndTrimsPresentValue() {
        assertThat(BoundedText.optional(null, "Details", 20)).isNull();
        assertThat(BoundedText.optional(" ", "Details", 20)).isNull();
        assertThat(BoundedText.optional("  updated  ", "Details", 20)).isEqualTo("updated");
    }

    @Test
    void presentValue_RejectsTextLongerThanMaximum() {
        assertThatThrownBy(() -> BoundedText.optional("toolong", "Details", 3))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Details cannot exceed 3 characters");
    }

    @Test
    void presentValue_RejectsControlCharacters() {
        assertThatThrownBy(() -> BoundedText.required("bad\nvalue", "Filename", 20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Filename contains invalid characters");
    }
}
