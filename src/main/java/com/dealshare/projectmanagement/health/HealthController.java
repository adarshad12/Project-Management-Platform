package com.dealshare.projectmanagement.health;

import java.sql.Connection;
import java.time.Instant;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final DataSource dataSource;
    private final RedisConnectionFactory redisConnectionFactory;

    public HealthController(DataSource dataSource, RedisConnectionFactory redisConnectionFactory) {
        this.dataSource = dataSource;
        this.redisConnectionFactory = redisConnectionFactory;
    }

    @GetMapping("/live")
    ResponseEntity<Map<String, Object>> live() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "timestamp", Instant.now().toString()
        ));
    }

    @GetMapping("/ready")
    ResponseEntity<Map<String, Object>> ready() {
        boolean databaseUp = canConnectToDatabase();
        boolean redisUp = canPingRedis();
        String status = databaseUp && redisUp ? "UP" : "DOWN";

        return ResponseEntity
                .status(databaseUp && redisUp ? 200 : 503)
                .body(Map.of(
                        "status", status,
                        "checks", Map.of(
                                "database", databaseUp ? "UP" : "DOWN",
                                "redis", redisUp ? "UP" : "DOWN"
                        ),
                        "timestamp", Instant.now().toString()
                ));
    }

    private boolean canConnectToDatabase() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(1);
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean canPingRedis() {
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            String response = connection.ping();
            return "PONG".equalsIgnoreCase(response);
        } catch (Exception ignored) {
            return false;
        }
    }
}
