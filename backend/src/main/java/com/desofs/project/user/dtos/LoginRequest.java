package com.desofs.project.user.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "username must not be blank")
    @Size(max = 50, message = "username must be less than or equal to 50 characters")
    private String username;

    @NotBlank(message = "password must not be blank")
    @Size(max = 128, message = "password must be less than or equal to 128 characters")
    private String password;
}
