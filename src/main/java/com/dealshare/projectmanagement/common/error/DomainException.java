package com.dealshare.projectmanagement.common.error;

import org.springframework.http.HttpStatus;

public class DomainException extends RuntimeException {

    private final ErrorCode code;
    private final HttpStatus status;

    public DomainException(ErrorCode code, HttpStatus status, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public ErrorCode code() {
        return code;
    }

    public HttpStatus status() {
        return status;
    }
}
