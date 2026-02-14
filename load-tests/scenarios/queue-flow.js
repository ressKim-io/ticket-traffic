import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE_URL, TEST_GAME_ID, THRESHOLDS } from '../lib/config.js';
import { signup, authHeaders } from '../lib/helpers.js';

/**
 * Queue entry scenario: simulates users entering the virtual waiting room.
 * Tests queue-service capacity under load.
 */
export const options = {
  scenarios: {
    queue_entry: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 50 },   // Ramp up
        { duration: '1m', target: 100 },    // Steady state
        { duration: '30s', target: 200 },   // Spike
        { duration: '1m', target: 200 },    // Sustained spike
        { duration: '30s', target: 0 },     // Ramp down
      ],
    },
  },
  thresholds: THRESHOLDS,
};

export default function () {
  const vu = `loadtest_queue_${__VU}_${__ITER}`;
  const token = signup(vu, 'Password123!');
  if (!token) return;

  const hdrs = authHeaders(token);

  // Enter queue
  const enterRes = http.post(
    `${BASE_URL}/api/v1/queue/enter`,
    JSON.stringify({ gameId: TEST_GAME_ID }),
    { headers: hdrs, tags: { name: 'queue_enter' } }
  );

  check(enterRes, {
    'queue enter 200': (r) => r.status === 200,
    'queue position exists': (r) => {
      const body = JSON.parse(r.body);
      return body.data?.position > 0;
    },
  });

  // Poll queue status
  for (let i = 0; i < 5; i++) {
    sleep(2);
    const statusRes = http.get(
      `${BASE_URL}/api/v1/queue/${TEST_GAME_ID}/status`,
      { headers: hdrs, tags: { name: 'queue_status' } }
    );

    check(statusRes, {
      'queue status 200': (r) => r.status === 200,
    });

    const status = JSON.parse(statusRes.body);
    if (status.data?.status === 'ENTERED') break;
  }

  sleep(1);
}
