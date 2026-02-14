"use client";

import { useMutation } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import { apiClient } from "@/lib";
import { useAuthStore } from "@/stores";
import type {
  ApiResponse,
  LoginRequest,
  SignupRequest,
  TokenResponse,
  MemberResponse,
} from "@/types";

export function useLogin() {
  const router = useRouter();
  const { setTokens, setUser } = useAuthStore();

  return useMutation({
    mutationFn: async (req: LoginRequest) => {
      const { data } = await apiClient.post<ApiResponse<TokenResponse>>(
        "/auth/login",
        req
      );
      return data;
    },
    onSuccess: (res) => {
      if (res.success && res.data) {
        setTokens(res.data.accessToken, res.data.refreshToken);
        // Decode user info from JWT payload
        const payload = parseJwtPayload(res.data.accessToken);
        if (payload) {
          setUser({
            id: payload.sub ? Number(payload.sub) : 0,
            email: payload.email ?? "",
            name: payload.name ?? "",
            role: payload.role === "ADMIN" ? "ADMIN" : "USER",
          });
        }
        router.push("/games");
      }
    },
  });
}

export function useSignup() {
  const router = useRouter();

  return useMutation({
    mutationFn: async (req: SignupRequest) => {
      const { data } = await apiClient.post<ApiResponse<MemberResponse>>(
        "/auth/signup",
        req
      );
      return data;
    },
    onSuccess: (res) => {
      if (res.success) {
        router.push("/login?registered=true");
      }
    },
  });
}

export function useLogout() {
  const { logout } = useAuthStore();
  const router = useRouter();

  return useMutation({
    mutationFn: async () => {
      await apiClient.post("/auth/logout");
    },
    onSettled: () => {
      logout();
      router.push("/");
    },
  });
}

function parseJwtPayload(
  token: string
): Record<string, string> | null {
  try {
    const base64Url = token.split(".")[1];
    const base64 = base64Url.replace(/-/g, "+").replace(/_/g, "/");
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split("")
        .map((c) => "%" + ("00" + c.charCodeAt(0).toString(16)).slice(-2))
        .join("")
    );
    return JSON.parse(jsonPayload);
  } catch {
    return null;
  }
}
