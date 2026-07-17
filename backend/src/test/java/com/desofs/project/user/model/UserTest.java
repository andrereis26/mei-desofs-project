package com.desofs.project.user.model;

import com.desofs.project.department.model.Department;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

    @Test
    void builder_WithValidStringValues_CreatesUserWithNormalizedValues() {
        User user = User.builder()
                .username(" alice ")
                .email(" alice@example.com ")
                .password(" encoded-password ")
                .build();

        assertThat(user.getUsername()).isEqualTo("alice");
        assertThat(user.getEmail()).isEqualTo("alice@example.com");
        assertThat(user.getPassword()).isEqualTo("encoded-password");
        assertThat(user.getRole()).isEqualTo(Role.USER);
        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(user.getDepartments()).isEmpty();
    }

    @Test
    void builder_WithExplicitRole_CreatesUserWithThatRole() {
        User user = validUserBuilder()
                .role(Role.ADMIN)
                .build();

        assertThat(user.getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    void builder_WithNullRole_DefaultsToUserRole() {
        User user = validUserBuilder()
                .role(null)
                .build();

        assertThat(user.getRole()).isEqualTo(Role.USER);
    }

    @Test
    void builder_WithProvidedCreatedAt_PreservesCreatedAt() {
        LocalDateTime createdAt = LocalDateTime.now().minusDays(1);

        User user = validUserBuilder()
                .createdAt(createdAt)
                .build();

        assertThat(user.getCreatedAt()).isEqualTo(createdAt);
    }



    @Test
    void builder_WithNullUsername_RejectsUser() {
        assertThatThrownBy(() -> User.builder()
                .username((String) null)
                .email("alice@example.com")
                .password("encoded-password")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Username is required");
    }

    @Test
    void builder_WithBlankUsername_RejectsUser() {
        assertThatThrownBy(() -> User.builder()
                .username(" ")
                .email("alice@example.com")
                .password("encoded-password")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Username is required");
    }

    @Test
    void builder_WithTooShortUsername_RejectsUser() {
        assertThatThrownBy(() -> User.builder()
                .username("ab")
                .email("alice@example.com")
                .password("encoded-password")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Username must be between 3 and 50 characters");
    }

    @Test
    void builder_WithMaximumLengthUsername_CreatesUser() {
        String username = "a".repeat(50);

        User user = User.builder()
                .username(username)
                .email("alice@example.com")
                .password("encoded-password")
                .build();

        assertThat(user.getUsername()).isEqualTo(username);
    }

    @Test
    void builder_WithTooLongUsername_RejectsUser() {
        assertThatThrownBy(() -> User.builder()
                .username("a".repeat(51))
                .email("alice@example.com")
                .password("encoded-password")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Username cannot exceed 50 characters");
    }

    @Test
    void builder_WithNullEmail_RejectsUser() {
        assertThatThrownBy(() -> User.builder()
                .username("alice")
                .email((String) null)
                .password("encoded-password")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email is required");
    }

    @Test
    void builder_WithInvalidEmail_RejectsUser() {
        assertThatThrownBy(() -> User.builder()
                .username("alice")
                .email("invalid-email")
                .password("encoded-password")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email must be valid");
    }

    @Test
    void builder_WithEmailContainingWhitespace_RejectsUser() {
        assertThatThrownBy(() -> User.builder()
                .username("alice")
                .email("alice @example.com")
                .password("encoded-password")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email must be valid");
    }

    @Test
    void builder_WithEmailContainingMultipleAtSigns_RejectsUser() {
        assertThatThrownBy(() -> User.builder()
                .username("alice")
                .email("alice@@example.com")
                .password("encoded-password")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email must be valid");
    }

    @Test
    void builder_WithEmailDomainWithoutDot_RejectsUser() {
        assertThatThrownBy(() -> User.builder()
                .username("alice")
                .email("alice@example")
                .password("encoded-password")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email must be valid");
    }

    @Test
    void builder_WithMaximumLengthEmail_CreatesUser() {
        String email = "a".repeat(88) + "@example.com";

        User user = User.builder()
                .username("alice")
                .email(email)
                .password("encoded-password")
                .build();

        assertThat(user.getEmail()).isEqualTo(email);
    }

    @Test
    void builder_WithTooLongEmail_RejectsUser() {
        String email = "a".repeat(89) + "@example.com";

        assertThatThrownBy(() -> User.builder()
                .username("alice")
                .email(email)
                .password("encoded-password")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email cannot exceed 100 characters");
    }

    @Test
    void builder_WithNullPassword_RejectsUser() {
        assertThatThrownBy(() -> User.builder()
                .username("alice")
                .email("alice@example.com")
                .password((String) null)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Password is required");
    }

    @Test
    void builder_WithBlankPassword_RejectsUser() {
        assertThatThrownBy(() -> User.builder()
                .username("alice")
                .email("alice@example.com")
                .password(" ")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Password is required");
    }

    @Test
    void builder_WithMaximumLengthPassword_CreatesUser() {
        String password = "a".repeat(255);

        User user = User.builder()
                .username("alice")
                .email("alice@example.com")
                .password(password)
                .build();

        assertThat(user.getPassword()).isEqualTo(password);
    }

    @Test
    void builder_WithTooLongPassword_RejectsUser() {
        assertThatThrownBy(() -> User.builder()
                .username("alice")
                .email("alice@example.com")
                .password("a".repeat(256))
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Password cannot exceed 255 characters");
    }

    private User validUser() {
        return validUserBuilder().build();
    }

    private User.UserBuilder validUserBuilder() {
        return User.builder()
                .username("alice")
                .email("alice@example.com")
                .password("encoded-password");
    }
}
