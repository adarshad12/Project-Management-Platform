# Board Viewers Load Test

Date: 2026-06-10

Tool: k6 v2.0.0

Scenario: 100 concurrent board viewers for 1 minute.

Command:

```bash
BASE_URL=http://localhost:8081 \
PROJECT_ID=00000000-0000-0000-0000-000000000201 \
USER_EMAIL=viewer@example.com \
VUS=100 \
DURATION=1m \
k6 run --summary-export=load/results/board-viewers-100vus.json load/board-viewers.js
```

Environment:

- API: Spring Boot on `localhost:8081`
- Database: Docker PostgreSQL `pm-postgres` on host port `15432`
- Redis: Docker Redis `pm-redis` on host port `16379`
- Load-test rate limit override: `SECURITY_RATE_LIMIT_REQUESTS_PER_MINUTE=10000`

The rate-limit override keeps this run focused on board-read capacity. The default application rate limit is intentionally lower and is covered separately as a security feature.

Results:

| Metric | Result |
| --- | ---: |
| Max VUs | 100 |
| Duration | 1m |
| Iterations | 6000 |
| HTTP requests | 6001 |
| Failed requests | 0 |
| Failed request rate | 0.00% |
| Checks passed | 12001 / 12001 |
| Average latency | 15.49 ms |
| Median latency | 11.27 ms |
| p90 latency | 30.60 ms |
| p95 latency | 38.12 ms |
| Max latency | 120.10 ms |

Thresholds:

- `http_req_failed: rate<0.01` passed with `0.00%`.
- `http_req_duration: p(95)<500` passed with `38.12 ms`.

Conclusion:

The board endpoint handled 100 concurrent viewers with no request failures and p95 latency well below the 500 ms target.
