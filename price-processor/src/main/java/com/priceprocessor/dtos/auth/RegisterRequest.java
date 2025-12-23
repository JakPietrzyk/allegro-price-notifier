package com.priceprocessor.dtos.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 6, message = "Password should contain at least 6 characters")
        String password,

        String firstName,
        String lastName
) {
        public RegisterRequest(String email, String password) {
                this(email, password, "", "");
        }
}