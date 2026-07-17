package com.desofs.project.user.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {
    @NotBlank(message = "email must not be blank")
    @Email(message = "email must be valid")
    @Size(max = 100, message = "email must be less than or equal to 100 characters")
    private String email;
}
