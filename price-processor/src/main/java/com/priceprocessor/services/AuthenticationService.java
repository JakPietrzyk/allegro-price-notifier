package com.priceprocessor.services;

import com.priceprocessor.dtos.auth.AuthenticationRequest;
import com.priceprocessor.dtos.auth.AuthenticationResponse;
import com.priceprocessor.dtos.auth.RegisterRequest;
import com.priceprocessor.exceptions.InvalidCredentialsException;
import com.priceprocessor.exceptions.UserAlreadyExistsException;
import com.priceprocessor.models.Role;
import com.priceprocessor.models.User;
import com.priceprocessor.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final MetricsService metricsService;

    public AuthenticationResponse register(RegisterRequest request) {
        validateUniqueEmail(request.email());

        var user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(Role.USER)
                .build();
        repository.save(user);

        metricsService.incrementRegisterSuccess();
        var jwtToken = jwtService.generateToken(user);
        return new AuthenticationResponse(jwtToken);
    }

    private void validateUniqueEmail(String requestedEmail) {
        Optional<User> existingUser = repository.findByEmail(requestedEmail);

        if(existingUser.isPresent()) {
            metricsService.incrementRegisterFailure(UserAlreadyExistsException.class.getSimpleName());
            throw new UserAlreadyExistsException(requestedEmail);
        }
    }

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );
        } catch (AuthenticationException e) {
            metricsService.incrementLoginFailure(AuthenticationException.class.getSimpleName());
            throw new InvalidCredentialsException();
        }

        var user = repository.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);
        var jwtToken = jwtService.generateToken(user);

        metricsService.incrementLoginSuccess();
        return new AuthenticationResponse(jwtToken);
    }
}