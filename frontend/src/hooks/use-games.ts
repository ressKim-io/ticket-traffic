"use client";

import { useQuery } from "@tanstack/react-query";
import { apiClient } from "@/lib";
import type {
  ApiResponse,
  PageResponse,
  GameResponse,
  GameDetailResponse,
  GameListParams,
} from "@/types";

export function useGames(params: GameListParams = {}) {
  return useQuery({
    queryKey: ["games", params],
    queryFn: async () => {
      const { data } = await apiClient.get<
        ApiResponse<PageResponse<GameResponse>>
      >("/games", { params });
      return data;
    },
  });
}

export function useGameDetail(gameId: number) {
  return useQuery({
    queryKey: ["game", gameId],
    queryFn: async () => {
      const { data } = await apiClient.get<ApiResponse<GameDetailResponse>>(
        `/games/${gameId}`
      );
      return data;
    },
    enabled: gameId > 0,
  });
}
