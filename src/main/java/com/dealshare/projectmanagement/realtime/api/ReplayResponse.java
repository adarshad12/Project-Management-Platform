package com.dealshare.projectmanagement.realtime.api;

import java.util.List;

public record ReplayResponse(List<RealTimeEventResponse> events) {
}
