package com.desofs.project.user.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank(message = "username must not be blank")
    @Size(min = 3, max = 50, message = "username must be between 3 and 50 characters")
    private String username;

    @NotBlank(message = "email must not be blank")
    @Email(message = "email must be valid")
    @Size(max = 100, message = "email must be less than or equal to 100 characters")
    private String email;

    @NotBlank(message = "password must not be blank")
    @Size(min = 8, max = 128, message = "password must be between 8 and 128 characters")
    private String password;
}
