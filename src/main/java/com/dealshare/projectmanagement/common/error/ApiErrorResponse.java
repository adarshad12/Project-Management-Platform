package com.dealshare.projectmanagement.common.error;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(
        String code,
        String message,
        List<String> details,
        String correlationId,
        Instant timestamp
) {
}
