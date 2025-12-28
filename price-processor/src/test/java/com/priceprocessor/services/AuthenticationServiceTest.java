package com.priceprocessor.services;

import com.priceprocessor.dtos.auth.AuthenticationRequest;
import com.priceprocessor.dtos.auth.AuthenticationResponse;
import com.priceprocessor.dtos.auth.RegisterRequest;
import com.priceprocessor.exceptions.InvalidCredentialsException;
import com.priceprocessor.exceptions.UserAlreadyExistsException;
import com.priceprocessor.models.Role;
import com.priceprocessor.models.User;
import com.priceprocessor.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
    @Mock
    private MetricsService metricsService;

    @InjectMocks
    private AuthenticationService service;

    @Test
    void shouldRegisterUser_WhenRequestIsValid() {
        // Arrange
        String email = "john@example.com";
        String password = "securePassword";
        String encodedPassword = "encoded_securePassword";
        String jwtToken = "jwt_token_123";

        RegisterRequest request = new RegisterRequest(email, password, "", "");

        when(passwordEncoder.encode(password)).thenReturn(encodedPassword);
        when(jwtService.generateToken(any(User.class))).thenReturn(jwtToken);

        // Act
        AuthenticationResponse response = service.register(request);

        // Assert
        assertThat(response.accessToken()).isEqualTo(jwtToken);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(repository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getEmail()).isEqualTo(email);
        assertThat(savedUser.getPassword()).isEqualTo(encodedPassword);
        assertThat(savedUser.getRole()).isEqualTo(Role.USER);
    }

    @Test
    void shouldThrowException_WhenRegisteringExistingUser() {
        // Arrange
        String email = "john@example.com";
        RegisterRequest request = new RegisterRequest(email, "pass", "", "");


        when(repository.findByEmail(email)).thenReturn(Optional.of(new User()));

        // Act & Assert
        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining(email);

        verify(repository, never()).save(any());
    }

    @Test
    void shouldAuthenticateUser_WhenCredentialsAreCorrect() {
        // Arrange
        String email = "john@example.com";
        String password = "securePassword";
        String jwtToken = "jwt_token_123";

        AuthenticationRequest request = new AuthenticationRequest(email, password);
        User user = User.builder()
                .email(email)
                .password("encoded_pass")
                .role(Role.USER)
                .build();

        when(repository.findByEmail(email)).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn(jwtToken);

        // Act
        AuthenticationResponse response = service.authenticate(request);

        // Assert
        assertThat(response.accessToken()).isEqualTo(jwtToken);

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void shouldThrowException_WhenAuthenticationManagerFails() {
        // Arrange
        AuthenticationRequest request = new AuthenticationRequest("wrong@mail.com", "wrong_pass");

        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager)
                .authenticate(any(UsernamePasswordAuthenticationToken.class));

        // Act & Assert
        assertThatThrownBy(() -> service.authenticate(request))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(jwtService, never()).generateToken(any());
        verify(repository, never()).findByEmail(any());
    }

    @Test
    void shouldThrowException_WhenUserNotFoundInDatabaseAfterAuth() {
        // Arrange
        String email = "ghost@example.com";
        AuthenticationRequest request = new AuthenticationRequest(email, "pass");

        when(repository.findByEmail(email)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.authenticate(request))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}