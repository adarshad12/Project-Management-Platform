package com.dealshare.projectmanagement.security;

import com.dealshare.projectmanagement.common.error.DomainException;
import com.dealshare.projectmanagement.common.error.ErrorCode;
import com.dealshare.projectmanagement.infrastructure.persistence.entity.UserEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder BASE64_URL_DECODER = Base64.getUrlDecoder();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;
    private final String issuer;
    private final long ttlSeconds;
    private final byte[] secret;

    public JwtService(
            ObjectMapper objectMapper,
            @Value("${security.jwt.secret:local-dev-secret-change-me}") String secret,
            @Value("${security.jwt.issuer:project-management-platform}") String issuer,
            @Value("${security.jwt.ttl-seconds:86400}") long ttlSeconds
    ) {
        this.objectMapper = objectMapper;
        this.issuer = issuer;
        this.ttlSeconds = ttlSeconds;
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    public String issueToken(UserEntity user) {
        Instant now = Instant.now();
        Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("iss", issuer);
        payload.put("sub", user.id().toString());
        payload.put("email", user.email());
        payload.put("name", user.displayName());
        payload.put("iat", now.getEpochSecond());
        payload.put("exp", now.plusSeconds(ttlSeconds).getEpochSecond());

        String unsigned = base64Json(header) + "." + base64Json(payload);
        return unsigned + "." + sign(unsigned);
    }

    public AuthenticatedUser validate(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw invalidToken();
        }

        String unsigned = parts[0] + "." + parts[1];
        if (!constantTimeEquals(sign(unsigned), parts[2])) {
            throw invalidToken();
        }

        Map<String, Object> payload = readJson(new String(BASE64_URL_DECODER.decode(parts[1]), StandardCharsets.UTF_8));
        if (!issuer.equals(payload.get("iss"))) {
            throw invalidToken();
        }
        long exp = ((Number) payload.get("exp")).longValue();
        if (Instant.now().getEpochSecond() >= exp) {
            throw new DomainException(ErrorCode.UNAUTHENTICATED, HttpStatus.UNAUTHORIZED, "JWT is expired");
        }

        return new AuthenticatedUser(
                UUID.fromString(String.valueOf(payload.get("sub"))),
                String.valueOf(payload.get("email")),
                String.valueOf(payload.get("name"))
        );
    }

    private String base64Json(Object value) {
        try {
            return BASE64_URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(value));
        } catch (JsonProcessingException exception) {
            throw new DomainException(ErrorCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, "Failed to write JWT");
        }
    }

    private Map<String, Object> readJson(String json) {
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (JsonProcessingException exception) {
            throw invalidToken();
        }
    }

    private String sign(String unsigned) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return BASE64_URL_ENCODER.encodeToString(mac.doFinal(unsigned.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new DomainException(ErrorCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, "Failed to sign JWT");
        }
    }

    private boolean constantTimeEquals(String left, String right) {
        return MessageDigestUtil.constantTimeEquals(left.getBytes(StandardCharsets.UTF_8), right.getBytes(StandardCharsets.UTF_8));
    }

    private DomainException invalidToken() {
        return new DomainException(ErrorCode.UNAUTHENTICATED, HttpStatus.UNAUTHORIZED, "JWT is invalid");
    }
}
