// Base configuration for K6 load tests
export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
export const WS_URL = __ENV.WS_URL || 'ws://localhost:8080';

// Test game configuration
export const TEST_GAME_ID = parseInt(__ENV.GAME_ID || '1');

// Thresholds matching SLA requirements
export const THRESHOLDS = {
  http_req_duration: ['p(95)<2000', 'p(99)<5000'],
  http_req_failed: ['rate<0.05'],
  http_reqs: ['rate>100'],
};
