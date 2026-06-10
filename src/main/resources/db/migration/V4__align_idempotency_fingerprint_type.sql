ALTER TABLE idempotency_keys
    ALTER COLUMN request_fingerprint TYPE VARCHAR(64);
