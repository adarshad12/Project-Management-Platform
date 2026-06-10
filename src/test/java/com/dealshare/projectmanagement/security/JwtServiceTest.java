package com.dealshare.projectmanagement.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dealshare.projectmanagement.common.error.DomainException;
import com.dealshare.projectmanagement.infrastructure.persistence.entity.UserEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService(
            new ObjectMapper(),
            "test-secret-with-enough-entropy",
            "test-issuer",
            3600
    );

    @Test
    void issuedTokenRoundTripsIntoAuthenticatedUser() {
        UserEntity user = new UserEntity(
                UUID.fromString("00000000-0000-0000-0000-000000000102"),
                "lead@example.com",
                "Ravi Lead",
                Instant.now()
        );

        AuthenticatedUser authenticated = jwtService.validate(jwtService.issueToken(user));

        assertThat(authenticated.userId()).isEqualTo(user.id());
        assertThat(authenticated.email()).isEqualTo("lead@example.com");
        assertThat(authenticated.displayName()).isEqualTo("Ravi Lead");
    }

    @Test
    void tamperedTokenIsRejected() {
        UserEntity user = new UserEntity(UUID.randomUUID(), "member@example.com", "Jane Smith", Instant.now());
        String token = jwtService.issueToken(user);

        assertThatThrownBy(() -> jwtService.validate(token + "x"))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("JWT is invalid");
    }
}
