package com.dealshare.projectmanagement.common.idempotency;

import com.dealshare.projectmanagement.common.error.DomainException;
import com.dealshare.projectmanagement.common.error.ErrorCode;
import com.dealshare.projectmanagement.infrastructure.persistence.entity.IdempotencyKeyEntity;
import com.dealshare.projectmanagement.infrastructure.persistence.repository.IdempotencyKeyJpaRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class IdempotencyService {

    private final IdempotencyKeyJpaRepository idempotencyKeys;
    private final ObjectMapper objectMapper;

    public IdempotencyService(IdempotencyKeyJpaRepository idempotencyKeys, ObjectMapper objectMapper) {
        this.idempotencyKeys = idempotencyKeys;
        this.objectMapper = objectMapper;
    }

    public <T> T execute(String operation, Object request, String key, Class<T> responseType, Supplier<T> supplier) {
        if (key == null || key.isBlank()) {
            return supplier.get();
        }
        String fingerprint = fingerprint(operation, request);
        return idempotencyKeys.findByIdempotencyKey(key)
                .map(existing -> replayOrReject(existing, fingerprint, responseType))
                .orElseGet(() -> {
                    T response = supplier.get();
                    idempotencyKeys.save(new IdempotencyKeyEntity(
                            UUID.randomUUID(),
                            key,
                            fingerprint,
                            200,
                            writeJson(response),
                            Instant.now(),
                            Instant.now().plus(24, ChronoUnit.HOURS)
                    ));
                    return response;
                });
    }

    private <T> T replayOrReject(IdempotencyKeyEntity existing, String fingerprint, Class<T> responseType) {
        if (!existing.requestFingerprint().equals(fingerprint)) {
            throw new DomainException(
                    ErrorCode.IDEMPOTENCY_CONFLICT,
                    HttpStatus.CONFLICT,
                    "Idempotency-Key was already used with a different request"
            );
        }
        if (!existing.hasStoredResponse()) {
            throw new DomainException(ErrorCode.CONFLICT, HttpStatus.CONFLICT, "Request is already in progress");
        }
        try {
            return objectMapper.readValue(existing.responseBody(), responseType);
        } catch (JsonProcessingException exception) {
            throw new DomainException(ErrorCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, "Failed to replay idempotent response");
        }
    }

    private String fingerprint(String operation, Object request) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((operation + ":" + writeJson(request)).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new DomainException(ErrorCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, "SHA-256 is unavailable");
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new DomainException(ErrorCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, "Failed to serialize JSON");
        }
    }
}
