package com.desofs.project.shared.exceptions;

public class EmptyFileException extends RuntimeException {

    public EmptyFileException() {
        super("Uploaded file is empty");
    }
}
