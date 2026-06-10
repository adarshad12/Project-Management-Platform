package com.dealshare.projectmanagement.infrastructure.persistence;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AdvisoryLockRepository {

    private final JdbcTemplate jdbcTemplate;

    public AdvisoryLockRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void lockSprint(UUID sprintId) {
        long high = sprintId.getMostSignificantBits();
        long low = sprintId.getLeastSignificantBits();
        jdbcTemplate.query("select pg_advisory_xact_lock(?, ?)", resultSet -> {
        }, (int) high, (int) low);
    }
}
