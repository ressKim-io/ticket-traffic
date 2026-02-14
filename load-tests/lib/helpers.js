import http from 'k6/http';
import { check } from 'k6';
import { BASE_URL } from './config.js';

const headers = { 'Content-Type': 'application/json' };

/**
 * Register a new user and return the auth token.
 */
export function signup(username, password) {
  const res = http.post(`${BASE_URL}/api/v1/auth/signup`, JSON.stringify({
    username,
    password,
    email: `${username}@loadtest.io`,
    nickname: username,
  }), { headers });

  check(res, { 'signup success': (r) => r.status === 200 || r.status === 201 });
  const body = JSON.parse(res.body);
  return body.data?.accessToken || null;
}

/**
 * Login and return the auth token.
 */
export function login(username, password) {
  const res = http.post(`${BASE_URL}/api/v1/auth/login`, JSON.stringify({
    username,
    password,
  }), { headers });

  check(res, { 'login success': (r) => r.status === 200 });
  const body = JSON.parse(res.body);
  return body.data?.accessToken || null;
}

/**
 * Get auth headers for a token.
 */
export function authHeaders(token) {
  return {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`,
  };
}
