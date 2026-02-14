import axios, { type AxiosError, type InternalAxiosRequestConfig } from "axios";
import type { ApiResponse } from "@/types";
import { useAuthStore } from "@/stores";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

export const apiClient = axios.create({
  baseURL: `${API_BASE_URL}/api/v1`,
  timeout: 10000,
  headers: {
    "Content-Type": "application/json",
  },
});

// Request interceptor: attach JWT token
apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = useAuthStore.getState().accessToken;
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor: handle token refresh on 401
apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<ApiResponse<null>>) => {
    const originalRequest = error.config;

    if (
      error.response?.status === 401 &&
      originalRequest &&
      !originalRequest.headers["X-Retry"]
    ) {
      const refreshToken = useAuthStore.getState().refreshToken;

      if (refreshToken) {
        try {
          const { data } = await axios.post<
            ApiResponse<{ accessToken: string; refreshToken: string }>
          >(`${API_BASE_URL}/api/v1/auth/refresh`, { refreshToken });

          if (data.success && data.data) {
            useAuthStore
              .getState()
              .setTokens(data.data.accessToken, data.data.refreshToken);

            originalRequest.headers.Authorization = `Bearer ${data.data.accessToken}`;
            originalRequest.headers["X-Retry"] = "true";
            return apiClient(originalRequest);
          }
        } catch {
          useAuthStore.getState().logout();
        }
      } else {
        useAuthStore.getState().logout();
      }
    }

    return Promise.reject(error);
  }
);
