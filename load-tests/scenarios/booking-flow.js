import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE_URL, TEST_GAME_ID, THRESHOLDS } from '../lib/config.js';
import { signup, authHeaders } from '../lib/helpers.js';

/**
 * Booking scenario: seat selection -> hold -> confirm payment.
 * Tests booking-service 3-tier lock under concurrent seat contention.
 */
export const options = {
  scenarios: {
    booking: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 20 },
        { duration: '2m', target: 50 },
        { duration: '1m', target: 100 },   // Peak concurrency
        { duration: '30s', target: 0 },
      ],
    },
  },
  thresholds: {
    ...THRESHOLDS,
    'http_req_duration{name:hold_seats}': ['p(95)<3000'],
    'http_req_duration{name:confirm_booking}': ['p(95)<5000'],
  },
};

export default function () {
  const vu = `loadtest_book_${__VU}_${__ITER}`;
  const token = signup(vu, 'Password123!');
  if (!token) return;

  const hdrs = authHeaders(token);

  // 1. Get available seats
  const seatsRes = http.get(
    `${BASE_URL}/api/v1/games/${TEST_GAME_ID}/seats`,
    { headers: hdrs, tags: { name: 'get_seats' } }
  );

  check(seatsRes, { 'get seats 200': (r) => r.status === 200 });

  const seatsBody = JSON.parse(seatsRes.body);
  const availableSeats = (seatsBody.data || []).filter(s => s.status === 'AVAILABLE');

  if (availableSeats.length === 0) {
    // No seats available, test is done
    return;
  }

  // Pick 1-2 random seats
  const numSeats = Math.min(Math.floor(Math.random() * 2) + 1, availableSeats.length);
  const selectedSeatIds = [];
  for (let i = 0; i < numSeats; i++) {
    const idx = Math.floor(Math.random() * availableSeats.length);
    selectedSeatIds.push(availableSeats.splice(idx, 1)[0].id);
  }

  // 2. Hold seats (creates booking)
  const holdRes = http.post(
    `${BASE_URL}/api/v1/bookings/hold`,
    JSON.stringify({
      gameId: TEST_GAME_ID,
      gameSeatIds: selectedSeatIds,
    }),
    { headers: hdrs, tags: { name: 'hold_seats' } }
  );

  const holdOk = check(holdRes, {
    'hold seats success': (r) => r.status === 200 || r.status === 201,
  });

  if (!holdOk) {
    // Seat already taken (expected under concurrency)
    return;
  }

  const holdBody = JSON.parse(holdRes.body);
  const bookingId = holdBody.data?.id;
  if (!bookingId) return;

  sleep(1);

  // 3. Confirm booking (triggers payment)
  const confirmRes = http.post(
    `${BASE_URL}/api/v1/bookings/${bookingId}/confirm`,
    null,
    { headers: hdrs, tags: { name: 'confirm_booking' } }
  );

  check(confirmRes, {
    'confirm booking success': (r) => r.status === 200,
  });

  sleep(0.5);
}
