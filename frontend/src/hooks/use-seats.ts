"use client";

import { useQuery, useMutation } from "@tanstack/react-query";
import { apiClient } from "@/lib";
import type {
  ApiResponse,
  GameSeatResponse,
  HoldSeatsRequest,
  BookingResponse,
} from "@/types";

/**
 * Fetch seats for a section in a game.
 */
export function useGameSeats(gameId: number, sectionId: number | null) {
  return useQuery({
    queryKey: ["game-seats", gameId, sectionId],
    queryFn: async () => {
      const { data } = await apiClient.get<ApiResponse<GameSeatResponse[]>>(
        `/games/${gameId}/seats`,
        { params: { sectionId } }
      );
      return data;
    },
    enabled: gameId > 0 && sectionId != null && sectionId > 0,
    refetchInterval: 10000, // Poll every 10s for seat status updates
  });
}

/**
 * Hold selected seats (creates a PENDING booking).
 */
export function useHoldSeats() {
  return useMutation({
    mutationFn: async (req: HoldSeatsRequest) => {
      const { data } = await apiClient.post<ApiResponse<BookingResponse>>(
        "/bookings/hold",
        req
      );
      return data;
    },
  });
}
