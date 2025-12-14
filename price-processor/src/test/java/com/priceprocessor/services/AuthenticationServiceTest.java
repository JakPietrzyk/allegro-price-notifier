package com.priceprocessor.services;

import com.priceprocessor.dtos.auth.AuthenticationRequest;
import com.priceprocessor.dtos.auth.AuthenticationResponse;
import com.priceprocessor.dtos.auth.RegisterRequest;
import com.priceprocessor.models.Role;
import com.priceprocessor.models.User;
import com.priceprocessor.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserRepository repository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthenticationService authService;

    @Test
    void shouldRegisterUserSuccessfully() {
        // Arrange
        RegisterRequest request = new RegisterRequest("test@test.com", "password123", "Jan", "Kowalski");
        User savedUser = User.builder()
                .email("test@test.com")
                .role(Role.USER)
                .build();

        when(passwordEncoder.encode(request.password())).thenReturn("hashedPassword");
        when(jwtService.generateToken(any(User.class))).thenReturn("fake-jwt-token");

        // Act
        AuthenticationResponse response = authService.register(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("fake-jwt-token");

        verify(passwordEncoder).encode("password123");
        verify(repository).save(any(User.class));
    }

    @Test
    void shouldAuthenticateUserSuccessfully() {
        // Arrange
        AuthenticationRequest request = new AuthenticationRequest("test@test.com", "password123");
        User user = User.builder().email("test@test.com").build();

        when(repository.findByEmail(request.email())).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("fake-jwt-token");

        // Act
        AuthenticationResponse response = authService.authenticate(request);

        // Assert
        assertThat(response.accessToken()).isEqualTo("fake-jwt-token");

        verify(authenticationManager).authenticate(any());
    }
}