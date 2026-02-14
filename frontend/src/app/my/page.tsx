"use client";

import { useEffect } from "react";
import Link from "next/link";
import { AuthGuard } from "@/components/auth";
import { BookingCard } from "@/components/booking";
import { useUserBookings } from "@/hooks";
import { getErrorMessage } from "@/lib";

function MyBookings() {
  const { data, isLoading, isError, error } = useUserBookings();
  const bookings = data?.data ?? [];

  useEffect(() => {
    document.title = "My Bookings | SportsTix";
  }, []);

  if (isLoading) {
    return (
      <div className="flex justify-center py-12" role="status" aria-live="polite">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary-200 border-t-primary-600" />
        <span className="sr-only">Loading your bookings...</span>
      </div>
    );
  }

  if (isError) {
    return (
      <div className="py-12 text-center">
        <p className="font-medium text-red-800">Failed to load bookings.</p>
        <p className="mt-1 text-sm text-red-600">{getErrorMessage(error)}</p>
      </div>
    );
  }

  if (bookings.length === 0) {
    return (
      <div className="py-12 text-center">
        <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-gray-100">
          <svg
            className="h-8 w-8 text-gray-400"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={1.5}
              d="M15 5v2m0 4v2m0 4v2M5 5a2 2 0 00-2 2v3a2 2 0 110 4v3a2 2 0 002 2h14a2 2 0 002-2v-3a2 2 0 110-4V7a2 2 0 00-2-2H5z"
            />
          </svg>
        </div>
        <h2 className="mt-4 text-lg font-bold text-gray-900">
          No bookings yet
        </h2>
        <p className="mt-1 text-sm text-gray-500">
          Find a game and book your seats!
        </p>
        <Link
          href="/games"
          className="mt-4 inline-block rounded-lg bg-primary-600 px-5 py-2.5 text-sm font-semibold text-white hover:bg-primary-700"
        >
          Browse Games
        </Link>
      </div>
    );
  }

  return (
    <div className="space-y-3">
      {bookings.map((booking) => (
        <BookingCard key={booking.bookingId} booking={booking} />
      ))}
    </div>
  );
}

export default function MyPage() {
  return (
    <div className="mx-auto max-w-2xl px-4 py-8 sm:px-6 lg:px-8">
      <h1 className="text-2xl font-bold text-gray-900">My Bookings</h1>
      <p className="mt-1 text-sm text-gray-500">
        View and manage your ticket bookings.
      </p>
      <div className="mt-6">
        <AuthGuard>
          <MyBookings />
        </AuthGuard>
      </div>
    </div>
  );
}
