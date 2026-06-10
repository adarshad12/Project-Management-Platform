package com.dealshare.projectmanagement.security.api;

import com.dealshare.projectmanagement.common.error.DomainException;
import com.dealshare.projectmanagement.common.error.ErrorCode;
import com.dealshare.projectmanagement.infrastructure.persistence.entity.UserEntity;
import com.dealshare.projectmanagement.infrastructure.persistence.repository.UserJpaRepository;
import com.dealshare.projectmanagement.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserJpaRepository users;
    private final JwtService jwtService;

    public AuthController(UserJpaRepository users, JwtService jwtService) {
        this.users = users;
        this.jwtService = jwtService;
    }

    @PostMapping("/token")
    TokenResponse token(@Valid @RequestBody TokenRequest request) {
        UserEntity user = users.findByEmail(request.email())
                .orElseThrow(() -> new DomainException(ErrorCode.UNAUTHENTICATED, HttpStatus.UNAUTHORIZED, "User is not available for token issuance"));
        return new TokenResponse("Bearer", jwtService.issueToken(user), user.id(), user.email(), user.displayName());
    }
}
