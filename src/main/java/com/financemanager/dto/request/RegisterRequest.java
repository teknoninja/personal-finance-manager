package com.financemanager.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "username is required")
    @Email(message = "username must be a valid email address")
    private String username;

    @NotBlank(message = "password is required")
    @Size(min = 8, message = "password must be at least 8 characters long")
    private String password;

    @NotBlank(message = "fullName is required")
    private String fullName;

    @NotBlank(message = "phoneNumber is required")
    @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "phoneNumber must be a valid contact number")
    private String phoneNumber;
}
