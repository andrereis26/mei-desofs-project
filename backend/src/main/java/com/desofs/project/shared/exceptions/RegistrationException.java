package com.desofs.project.shared.exceptions;

public class RegistrationException extends RuntimeException {

    public RegistrationException() {
        super("Invalid registration request");
    }
}
