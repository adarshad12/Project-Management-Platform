package com.dealshare.projectmanagement.realtime.application;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class WebSocketLifecycleListener {

    private final RealTimePresenceService presenceService;
    private final AtomicInteger activeConnections = new AtomicInteger();

    public WebSocketLifecycleListener(RealTimePresenceService presenceService, MeterRegistry meterRegistry) {
        this.presenceService = presenceService;
        Gauge.builder("websocket.connections.active", activeConnections, AtomicInteger::get)
                .description("Active WebSocket/STOMP connection count")
                .register(meterRegistry);
    }

    @EventListener
    public void onConnect(SessionConnectEvent ignored) {
        activeConnections.incrementAndGet();
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        activeConnections.updateAndGet(value -> Math.max(0, value - 1));
        presenceService.disconnect(event.getSessionId());
    }
}
