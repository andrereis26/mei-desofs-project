package com.desofs.project.shared.controller;

import com.desofs.project.shared.exceptions.*;
import com.giffing.bucket4j.spring.boot.starter.context.RateLimitException;
import com.desofs.project.shared.exception.RateLimitExceededException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({BadCredentialsException.class, AuthenticationException.class})
    public ProblemDetail handleAuthenticationException(Exception ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        problem.setType(URI.create("about:authentication-failed"));
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationException(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, errors);
        problem.setType(URI.create("about:validation-error"));
        return problem;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolationException(ConstraintViolationException ex) {
        String errors = ex.getConstraintViolations().stream()
                .map(violation -> violation.getMessage())
                .collect(Collectors.joining(", "));
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, errors);
        problem.setType(URI.create("about:validation-error"));
        return problem;
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Invalid request parameter");
        problem.setType(URI.create("about:validation-error"));
        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("about:conflict"));
        return problem;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleSecurityException(AccessDeniedException  ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Access denied");
        problem.setType(URI.create("about:access-denied"));
        return problem;
    }

    @ExceptionHandler(DocumentNotFoundException.class)
    public ProblemDetail handleDocumentNotFound(
            DocumentNotFoundException ex) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                "Document not found"
        );

        problem.setType(URI.create("about:document-not-found"));
        problem.setProperty("documentId", ex.getDocumentId());

        return problem;
    }

    @ExceptionHandler(DepartmentNotFoundException.class)
    public ProblemDetail handleDepartmentNotFound(
            DepartmentNotFoundException ex) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                "Department not found"
        );

        problem.setType(URI.create("about:department-not-found"));
        problem.setProperty("departmentId", ex.getDepartmentId());

        return problem;
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ProblemDetail handleUserNotFound(
            UserNotFoundException ex) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                "User not found"
        );

        problem.setType(URI.create("about:user-not-found"));
        return problem;
    }

    @ExceptionHandler(EmptyFileException.class)
    public ProblemDetail handleEmptyFile(EmptyFileException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );

        problem.setType(URI.create("about:empty-file"));

        return problem;
    }

    @ExceptionHandler(FileTooLargeException.class)
    public ProblemDetail handleFileTooLarge(FileTooLargeException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.PAYLOAD_TOO_LARGE,
                ex.getMessage()
        );

        problem.setType(URI.create("about:file-too-large"));

        return problem;
    }

    @ExceptionHandler(UnsupportedDocumentTypeException.class)
    public ProblemDetail handleUnsupportedDocumentType(UnsupportedDocumentTypeException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                ex.getMessage()
        );

        problem.setType(URI.create("about:unsupported-document-type"));

        return problem;
    }

    @ExceptionHandler(UnsafePdfException.class)
    public ProblemDetail handleUnsafePdf(UnsafePdfException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );

        problem.setType(URI.create("about:unsafe-pdf"));

        return problem;
    }

    @ExceptionHandler(DepartmentNameAlreadyExistsException.class)
    public ProblemDetail handleDepartmentNameConflict(DepartmentNameAlreadyExistsException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                ex.getMessage()
        );

        problem.setType(URI.create("about:department-name-conflict"));

        return problem;
    }

    @ExceptionHandler(ManagersNotFoundException.class)
    public ProblemDetail handleUsersNotFound(
            ManagersNotFoundException ex) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                "Some users were not found"
        );

        problem.setType(URI.create("about:users-not-found"));
        problem.setProperty("missingUserIds", ex.getMissingUserIds());

        return problem;
    }

    @ExceptionHandler(InvalidFilePathException.class)
    public ProblemDetail handleInvalidFilePath(InvalidFilePathException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );

        problem.setType(URI.create("about:invalid-file-path"));

        return problem;
    }

    @ExceptionHandler(RegistrationException.class)
    public ProblemDetail handleRegistrationException(RegistrationException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                ex.getMessage()
        );

        problem.setType(URI.create("about:registration-error"));
        return problem;
    }

    @ExceptionHandler({RateLimitExceededException.class, RateLimitException.class})
    public ProblemDetail handleRateLimitExceeded(RuntimeException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, "Too many requests");
        problem.setType(URI.create("about:rate-limit"));
        return problem;
    }
}
