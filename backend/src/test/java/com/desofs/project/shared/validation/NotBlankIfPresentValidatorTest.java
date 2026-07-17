package com.desofs.project.shared.validation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NotBlankIfPresentValidatorTest {

    private final NotBlankIfPresentValidator validator = new NotBlankIfPresentValidator();

    @Test
    void isValid_AllowsNullAndNonBlankValuesOnly() {
        assertThat(validator.isValid(null, null)).isTrue();
        assertThat(validator.isValid(" value ", null)).isTrue();
        assertThat(validator.isValid(" ", null)).isFalse();
    }
}
