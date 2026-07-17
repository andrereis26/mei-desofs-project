package com.desofs.project.shared.controller;

import com.desofs.project.shared.exception.RateLimitExceededException;
import com.desofs.project.shared.exceptions.DepartmentNameAlreadyExistsException;
import com.desofs.project.shared.exceptions.DepartmentNotFoundException;
import com.desofs.project.shared.exceptions.DocumentNotFoundException;
import com.desofs.project.shared.exceptions.EmptyFileException;
import com.desofs.project.shared.exceptions.FileTooLargeException;
import com.desofs.project.shared.exceptions.InvalidFilePathException;
import com.desofs.project.shared.exceptions.ManagersNotFoundException;
import com.desofs.project.shared.exceptions.RegistrationException;
import com.desofs.project.shared.exceptions.UserNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleAuthenticationException_ReturnsUnauthorizedProblem() {
        ProblemDetail problem = handler.handleAuthenticationException(new BadCredentialsException("bad"));

        assertProblem(problem, HttpStatus.UNAUTHORIZED, "Invalid credentials", "about:authentication-failed");
    }

    @Test
    void handleValidationException_JoinsFieldErrors() throws NoSuchMethodException {
        Method method = GlobalExceptionHandlerTest.class.getDeclaredMethod("validationTarget", String.class);
        MethodParameter parameter = new MethodParameter(method, 0);
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "name", "name is required"));
        bindingResult.addError(new FieldError("request", "email", "email is invalid"));

        ProblemDetail problem = handler.handleValidationException(
                new MethodArgumentNotValidException(parameter, bindingResult));

        assertProblem(problem, HttpStatus.BAD_REQUEST, "name is required, email is invalid", "about:validation-error");
    }

    @Test
    void handleConstraintViolationException_JoinsViolationMessages() {
        ConstraintViolation<?> first = mock(ConstraintViolation.class);
        ConstraintViolation<?> second = mock(ConstraintViolation.class);
        when(first.getMessage()).thenReturn("page must be positive");
        when(second.getMessage()).thenReturn("size is too large");

        ProblemDetail problem = handler.handleConstraintViolationException(
                new ConstraintViolationException(Set.of(first, second)));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getDetail()).contains("page must be positive").contains("size is too large");
        assertThat(problem.getType()).isEqualTo(URI.create("about:validation-error"));
    }

    @Test
    void handleTypeMismatchException_ReturnsValidationProblem() {
        ProblemDetail problem = handler.handleTypeMismatchException(mock(MethodArgumentTypeMismatchException.class));

        assertProblem(problem, HttpStatus.BAD_REQUEST, "Invalid request parameter", "about:validation-error");
    }

    @Test
    void handleConflictAndSecurityExceptions_ReturnExpectedProblems() {
        assertProblem(handler.handleIllegalArgument(new IllegalArgumentException("bad state")),
                HttpStatus.CONFLICT, "bad state", "about:conflict");
        assertProblem(handler.handleSecurityException(new AccessDeniedException("denied")),
                HttpStatus.FORBIDDEN, "Access denied", "about:access-denied");
        assertProblem(handler.handleRegistrationException(new RegistrationException()),
                HttpStatus.CONFLICT, "Invalid registration request", "about:registration-error");
    }

    @Test
    void handleDomainNotFoundExceptions_AddsIdentifiers() {
        UUID documentId = UUID.randomUUID();
        UUID departmentId = UUID.randomUUID();

        ProblemDetail documentProblem = handler.handleDocumentNotFound(new DocumentNotFoundException(documentId));
        ProblemDetail departmentProblem = handler.handleDepartmentNotFound(
                new DepartmentNotFoundException(departmentId.toString()));
        ProblemDetail userProblem = handler.handleUserNotFound(new UserNotFoundException());

        assertProblem(documentProblem, HttpStatus.NOT_FOUND, "Document not found", "about:document-not-found");
        assertThat(documentProblem.getProperties()).containsEntry("documentId", documentId);
        assertProblem(departmentProblem, HttpStatus.NOT_FOUND, "Department not found", "about:department-not-found");
        assertThat(departmentProblem.getProperties()).containsEntry("departmentId", departmentId.toString());
        assertProblem(userProblem, HttpStatus.NOT_FOUND, "User not found", "about:user-not-found");
    }

    @Test
    void handleFileAndDepartmentExceptions_ReturnExpectedProblems() {
        assertProblem(handler.handleEmptyFile(new EmptyFileException()),
                HttpStatus.BAD_REQUEST, "Uploaded file is empty", "about:empty-file");
        assertProblem(handler.handleFileTooLarge(new FileTooLargeException()),
                HttpStatus.PAYLOAD_TOO_LARGE, "File exceeds the maximum allowed size", "about:file-too-large");
        assertProblem(handler.handleInvalidFilePath(new InvalidFilePathException()),
                HttpStatus.BAD_REQUEST, "Invalid file path", "about:invalid-file-path");
        assertProblem(handler.handleDepartmentNameConflict(new DepartmentNameAlreadyExistsException("Engineering")),
                HttpStatus.CONFLICT, "Department name already exists: Engineering", "about:department-name-conflict");
    }

    @Test
    void handleManagersNotFound_AddsMissingIds() {
        UUID missingId = UUID.randomUUID();

        ProblemDetail problem = handler.handleUsersNotFound(new ManagersNotFoundException(Set.of(missingId)));

        assertProblem(problem, HttpStatus.NOT_FOUND, "Some users were not found", "about:users-not-found");
        assertThat(problem.getProperties()).containsEntry("missingUserIds", Set.of(missingId));
    }

    @Test
    void handleRateLimitExceeded_ReturnsTooManyRequests() {
        ProblemDetail problem = handler.handleRateLimitExceeded(new RateLimitExceededException());

        assertProblem(problem, HttpStatus.TOO_MANY_REQUESTS, "Too many requests", "about:rate-limit");
    }

    private static void assertProblem(ProblemDetail problem, HttpStatus status, String detail, String type) {
        assertThat(problem.getStatus()).isEqualTo(status.value());
        assertThat(problem.getDetail()).isEqualTo(detail);
        assertThat(problem.getType()).isEqualTo(URI.create(type));
    }

    @SuppressWarnings("unused")
    private static void validationTarget(String value) {
    }
}
