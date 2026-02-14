"use client";

import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { apiClient } from "@/lib";
import type { ApiResponse, BookingResponse } from "@/types";

/**
 * Fetch all bookings for the authenticated user.
 */
export function useUserBookings() {
  return useQuery({
    queryKey: ["bookings"],
    queryFn: async () => {
      const { data } =
        await apiClient.get<ApiResponse<BookingResponse[]>>("/bookings");
      return data;
    },
  });
}

/**
 * Fetch booking details by ID.
 */
export function useBooking(
  bookingId: number,
  options?: { enabled?: boolean }
) {
  return useQuery({
    queryKey: ["booking", bookingId],
    queryFn: async () => {
      const { data } = await apiClient.get<ApiResponse<BookingResponse>>(
        `/bookings/${bookingId}`
      );
      return data;
    },
    enabled: bookingId > 0 && options?.enabled !== false,
    refetchInterval: (query) => {
      // Poll while PENDING to detect expiration or external confirmation
      const booking = query.state.data?.data;
      return booking?.status === "PENDING" ? 5000 : false;
    },
  });
}

/**
 * Confirm a PENDING booking (triggers payment via SAGA).
 */
export function useConfirmBooking() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (bookingId: number) => {
      const { data } = await apiClient.post<ApiResponse<BookingResponse>>(
        `/bookings/${bookingId}/confirm`
      );
      return data;
    },
    onSuccess: (res, bookingId) => {
      if (res.success) {
        queryClient.invalidateQueries({ queryKey: ["booking", bookingId] });
      }
    },
  });
}

/**
 * Cancel a booking.
 */
export function useCancelBooking() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (bookingId: number) => {
      const { data } = await apiClient.post<ApiResponse<BookingResponse>>(
        `/bookings/${bookingId}/cancel`
      );
      return data;
    },
    onSuccess: (res, bookingId) => {
      if (res.success) {
        queryClient.invalidateQueries({ queryKey: ["booking", bookingId] });
      }
    },
  });
}
