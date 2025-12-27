package com.priceprocessor.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.priceprocessor.dtos.auth.AuthenticationRequest;
import com.priceprocessor.dtos.auth.AuthenticationResponse;
import com.priceprocessor.dtos.auth.RegisterRequest;
import com.priceprocessor.exceptions.InvalidCredentialsException;
import com.priceprocessor.exceptions.UserAlreadyExistsException;
import com.priceprocessor.services.AuthenticationService;
import com.priceprocessor.services.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthenticationController.class, properties = {
        "spring.cloud.gcp.sql.enabled=false",
        "spring.cloud.gcp.core.enabled=false"
})
@AutoConfigureMockMvc(addFilters = false)
class AuthenticationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticationService service;

    @MockitoBean
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldRegisterUser_WhenRequestIsValid() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest("user@test.com", "password123");
        AuthenticationResponse response = new AuthenticationResponse("dummy-jwt-token");

        when(service.register(any(RegisterRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").value("dummy-jwt-token"));
    }

    @Test
    void shouldReturnConflict_WhenUserAlreadyExists() throws Exception {
        RegisterRequest request = new RegisterRequest("exists@test.com", "pass123", "", "");

        when(service.register(any()))
                .thenThrow(new UserAlreadyExistsException("exists@test.com"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.statusCode").value(409))
                .andExpect(jsonPath("$.message").value("User with email exists@test.com already exists"));
    }

    @Test
    void shouldAuthenticateUser_WhenCredentialsAreCorrect() throws Exception {
        // Arrange
        AuthenticationRequest request = new AuthenticationRequest("user@test.com", "password123");
        AuthenticationResponse response = new AuthenticationResponse("dummy-jwt-token");

        when(service.authenticate(any(AuthenticationRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").value("dummy-jwt-token"));
    }

    @Test
    void shouldReturnUnauthorized_WhenAuthenticationFails() throws Exception {
        AuthenticationRequest request = new AuthenticationRequest("wrong@test.com", "wrong");

        when(service.authenticate(any()))
                .thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized()) // Oczekujemy 401
                .andExpect(jsonPath("$.statusCode").value(401))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void shouldReturnBadRequest_WhenValidationFails() throws Exception {
        RegisterRequest invalidRequest = new RegisterRequest("", "", "", "");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }
}