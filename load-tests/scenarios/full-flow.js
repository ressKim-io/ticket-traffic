import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Counter, Trend } from 'k6/metrics';
import { BASE_URL, TEST_GAME_ID, THRESHOLDS } from '../lib/config.js';
import { signup, authHeaders } from '../lib/helpers.js';

/**
 * Full end-to-end flow: signup -> queue -> seat selection -> booking -> payment.
 * Simulates realistic user journey for a ticket sale event.
 */

const bookingSuccess = new Counter('booking_success');
const bookingFailed = new Counter('booking_failed');
const e2eDuration = new Trend('e2e_booking_duration');

export const options = {
  scenarios: {
    full_flow: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '1m', target: 30 },    // Warm up
        { duration: '2m', target: 100 },    // Ramp up
        { duration: '3m', target: 100 },    // Sustained load
        { duration: '1m', target: 200 },    // Peak spike
        { duration: '1m', target: 0 },      // Cool down
      ],
    },
  },
  thresholds: {
    ...THRESHOLDS,
    'booking_success': ['count>0'],
    'e2e_booking_duration': ['p(95)<15000'],
  },
};

export default function () {
  const startTime = Date.now();
  const vu = `loadtest_full_${__VU}_${__ITER}`;
  const hdrs = { 'Content-Type': 'application/json' };

  // Step 1: Signup
  let token;
  group('01_signup', () => {
    token = signup(vu, 'Password123!');
  });
  if (!token) return;
  const authHdrs = authHeaders(token);

  // Step 2: Browse games
  group('02_browse_games', () => {
    const res = http.get(`${BASE_URL}/api/v1/games`, {
      headers: authHdrs,
      tags: { name: 'list_games' },
    });
    check(res, { 'list games 200': (r) => r.status === 200 });
  });

  sleep(1 + Math.random() * 2); // Think time: browsing

  // Step 3: Enter queue
  group('03_enter_queue', () => {
    const res = http.post(
      `${BASE_URL}/api/v1/queue/enter`,
      JSON.stringify({ gameId: TEST_GAME_ID }),
      { headers: authHdrs, tags: { name: 'queue_enter' } }
    );
    check(res, { 'queue enter ok': (r) => r.status === 200 });
  });

  // Step 4: Wait in queue (poll)
  group('04_wait_queue', () => {
    for (let i = 0; i < 10; i++) {
      sleep(2);
      const res = http.get(
        `${BASE_URL}/api/v1/queue/${TEST_GAME_ID}/status`,
        { headers: authHdrs, tags: { name: 'queue_poll' } }
      );
      const body = JSON.parse(res.body);
      if (body.data?.status === 'ENTERED') break;
    }
  });

  // Step 5: Select seats
  let selectedSeatIds = [];
  group('05_select_seats', () => {
    const res = http.get(
      `${BASE_URL}/api/v1/games/${TEST_GAME_ID}/seats`,
      { headers: authHdrs, tags: { name: 'get_seats' } }
    );

    const seats = JSON.parse(res.body).data || [];
    const available = seats.filter(s => s.status === 'AVAILABLE');
    if (available.length > 0) {
      const idx = Math.floor(Math.random() * available.length);
      selectedSeatIds.push(available[idx].id);
    }
  });

  if (selectedSeatIds.length === 0) {
    bookingFailed.add(1);
    return;
  }

  sleep(0.5 + Math.random()); // Think time: reviewing seats

  // Step 6: Hold seats
  let bookingId;
  group('06_hold_seats', () => {
    const res = http.post(
      `${BASE_URL}/api/v1/bookings/hold`,
      JSON.stringify({ gameId: TEST_GAME_ID, gameSeatIds: selectedSeatIds }),
      { headers: authHdrs, tags: { name: 'hold_seats' } }
    );

    if (res.status === 200 || res.status === 201) {
      bookingId = JSON.parse(res.body).data?.id;
    }
  });

  if (!bookingId) {
    bookingFailed.add(1);
    return;
  }

  sleep(1 + Math.random() * 3); // Think time: confirming purchase

  // Step 7: Confirm (triggers payment)
  group('07_confirm', () => {
    const res = http.post(
      `${BASE_URL}/api/v1/bookings/${bookingId}/confirm`,
      null,
      { headers: authHdrs, tags: { name: 'confirm_booking' } }
    );

    if (check(res, { 'confirm ok': (r) => r.status === 200 })) {
      bookingSuccess.add(1);
    } else {
      bookingFailed.add(1);
    }
  });

  e2eDuration.add(Date.now() - startTime);
}
