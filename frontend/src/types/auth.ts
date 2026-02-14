export interface User {
  id: number;
  email: string;
  name: string;
  role: "USER" | "ADMIN";
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface SignupRequest {
  email: string;
  password: string;
  name: string;
}

export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn?: number;
}

export interface MemberResponse {
  id: number;
  email: string;
  name: string;
  role: string;
}

export interface RefreshRequest {
  refreshToken: string;
}
