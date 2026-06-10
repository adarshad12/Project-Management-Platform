import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  scenarios: {
    board_viewers: {
      executor: 'constant-vus',
      vus: Number(__ENV.VUS || 100),
      duration: __ENV.DURATION || '1m',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<500'],
  },
};

const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
const projectId = __ENV.PROJECT_ID || '00000000-0000-0000-0000-000000000201';
const email = __ENV.USER_EMAIL || 'viewer@example.com';

export function setup() {
  const response = http.post(
    `${baseUrl}/api/v1/auth/token`,
    JSON.stringify({ email }),
    { headers: { 'Content-Type': 'application/json' } }
  );
  check(response, { 'token issued': (res) => res.status === 200 });
  return { token: response.json('accessToken') };
}

export default function (data) {
  const response = http.get(`${baseUrl}/api/v1/projects/${projectId}/board`, {
    headers: { Authorization: `Bearer ${data.token}` },
  });
  check(response, {
    'board status is 200': (res) => res.status === 200,
    'board has columns': (res) => Array.isArray(res.json('columns')),
  });
  sleep(1);
}
