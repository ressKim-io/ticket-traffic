"use client";

import { useQuery } from "@tanstack/react-query";
import { apiClient } from "@/lib";
import type { ApiResponse, DashboardResponse, GameStatsResponse } from "@/types";

export function useDashboard() {
  return useQuery({
    queryKey: ["admin", "dashboard"],
    queryFn: async () => {
      const { data } = await apiClient.get<ApiResponse<DashboardResponse>>(
        "/admin/dashboard"
      );
      return data.data!;
    },
    refetchInterval: 30_000,
  });
}

export function useGameStats() {
  return useQuery({
    queryKey: ["admin", "gameStats"],
    queryFn: async () => {
      const { data } = await apiClient.get<
        ApiResponse<GameStatsResponse[]>
      >("/admin/games/stats");
      return data.data ?? [];
    },
    refetchInterval: 30_000,
  });
}
