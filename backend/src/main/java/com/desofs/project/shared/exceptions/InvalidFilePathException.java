package com.desofs.project.shared.exceptions;

public class InvalidFilePathException extends RuntimeException {
    public InvalidFilePathException() {
        super("Invalid file path");
    }
}