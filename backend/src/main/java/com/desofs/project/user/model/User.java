package com.desofs.project.user.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.desofs.project.department.model.Department;

@Getter
@EqualsAndHashCode
@NoArgsConstructor
public class User {
    private UUID id;
    private Username username;
    private EmailAddress email;
    private Password password;
    private Role role = Role.USER;
    private LocalDateTime createdAt = LocalDateTime.now();
    private Set<Department> departments = new HashSet<>();

    @Builder
    public User(
            UUID id,
            Username username,
            EmailAddress email,
            Password password,
            Role role,
            LocalDateTime createdAt,
            Set<Department> departments) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.password = password;
        this.role = role == null ? Role.USER : role;
        this.createdAt = createdAt == null ? LocalDateTime.now() : createdAt;
        this.departments = departments == null ? new HashSet<>() : new HashSet<>(departments);
    }

    public String getUsername() {
        return username == null ? null : username.value();
    }

    public String getEmail() {
        return email == null ? null : email.value();
    }

    public String getPassword() {
        return password == null ? null : password.value();
    }



    public void setEmail(String email) {
        this.email = EmailAddress.of(email);
    }



    public void setRole(Role role) {
        this.role = role == null ? Role.USER : role;
    }

    public void setDepartments(Set<Department> departments) {
        this.departments = departments == null ? new HashSet<>() : new HashSet<>(departments);
    }

    public static class UserBuilder {
        public UserBuilder username(String username) {
            this.username = Username.of(username);
            return this;
        }

        public UserBuilder email(String email) {
            this.email = EmailAddress.of(email);
            return this;
        }

        public UserBuilder password(String password) {
            this.password = Password.of(password);
            return this;
        }
    }
}
