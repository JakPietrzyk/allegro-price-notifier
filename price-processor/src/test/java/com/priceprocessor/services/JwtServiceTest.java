package com.priceprocessor.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    private JwtService jwtService;

    @Mock
    private UserDetails userDetails;

    private static final String SECRET_KEY = "NDIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTI=";
    private static final long EXPIRATION_TIME = 1000 * 60 * 60; // 1 godzina

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", SECRET_KEY);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", EXPIRATION_TIME);
    }

    @Test
    void shouldGenerateToken_AndExtractUsername() {
        // Arrange
        String username = "testUser";
        when(userDetails.getUsername()).thenReturn(username);

        // Act
        String token = jwtService.generateToken(userDetails);
        String extractedUsername = jwtService.extractUsername(token);

        // Assert
        assertThat(token).isNotNull().isNotEmpty();
        assertThat(extractedUsername).isEqualTo(username);
    }

    @Test
    void shouldValidateToken_WhenTokenIsValidAndUserMatches() {
        // Arrange
        String username = "testUser";
        when(userDetails.getUsername()).thenReturn(username);

        String token = jwtService.generateToken(userDetails);

        // Act
        boolean isValid = jwtService.isTokenValid(token, userDetails);

        // Assert
        assertThat(isValid).isTrue();
    }

    @Test
    void shouldInvalidateToken_WhenUsernameDoesNotMatch() {
        // Arrange
        when(userDetails.getUsername()).thenReturn("user1");

        String token = jwtService.generateToken(userDetails);

        UserDetails otherUser = org.mockito.Mockito.mock(UserDetails.class);
        when(otherUser.getUsername()).thenReturn("user2");

        // Act
        boolean isValid = jwtService.isTokenValid(token, otherUser);

        // Assert
        assertThat(isValid).isFalse();
    }

    @Test
    void shouldExtractCustomClaims() {
        // Arrange
        String username = "adminUser";
        when(userDetails.getUsername()).thenReturn(username);

        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("role", "ADMIN");
        extraClaims.put("id", 123);

        // Act
        String token = jwtService.generateToken(extraClaims, userDetails);

        String extractedRole = jwtService.extractClaim(token, claims -> claims.get("role", String.class));
        Integer extractedId = jwtService.extractClaim(token, claims -> claims.get("id", Integer.class));

        // Assert
        assertThat(extractedRole).isEqualTo("ADMIN");
        assertThat(extractedId).isEqualTo(123);
    }

    @Test
    void shouldDetectExpiredToken() {
        // Arrange
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", -1000L);

        when(userDetails.getUsername()).thenReturn("expiredUser");

        String token = jwtService.generateToken(userDetails);

        // Act & Assert
        assertThatThrownBy(() -> jwtService.isTokenValid(token, userDetails))
                .isInstanceOf(io.jsonwebtoken.ExpiredJwtException.class);
    }
}