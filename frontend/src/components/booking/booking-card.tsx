"use client";

import Link from "next/link";
import type { BookingResponse } from "@/types";

const statusConfig: Record<
  string,
  { label: string; bg: string; text: string; dot: string }
> = {
  PENDING: {
    label: "Pending",
    bg: "bg-yellow-50",
    text: "text-yellow-800",
    dot: "bg-yellow-400",
  },
  CONFIRMED: {
    label: "Confirmed",
    bg: "bg-green-50",
    text: "text-green-800",
    dot: "bg-green-400",
  },
  CANCELLED: {
    label: "Cancelled",
    bg: "bg-gray-50",
    text: "text-gray-600",
    dot: "bg-gray-400",
  },
};

interface BookingCardProps {
  booking: BookingResponse;
}

export function BookingCard({ booking }: BookingCardProps) {
  const config = statusConfig[booking.status] ?? statusConfig.CANCELLED;
  const date = new Date(booking.createdAt);
  const formattedDate = date.toLocaleDateString("ko-KR", {
    year: "numeric",
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });

  return (
    <Link
      href={`/my/${booking.bookingId}`}
      className="block rounded-xl border border-gray-200 bg-white p-5 transition-shadow hover:shadow-md focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2"
      aria-label={`Booking #${booking.bookingId}, ${config.label}, ${booking.seats.length} seats, ₩${booking.totalPrice.toLocaleString()}`}
    >
      <div className="flex items-start justify-between">
        <div>
          <p className="text-sm font-bold text-gray-900">
            Booking #{booking.bookingId}
          </p>
          <p className="mt-0.5 text-xs text-gray-500">{formattedDate}</p>
        </div>
        <span
          className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-medium ${config.bg} ${config.text}`}
        >
          <span className={`h-1.5 w-1.5 rounded-full ${config.dot}`} />
          {config.label}
        </span>
      </div>

      <div className="mt-3 flex items-center justify-between">
        <p className="text-sm text-gray-600">
          {booking.seats.length} seat{booking.seats.length !== 1 ? "s" : ""}
        </p>
        <p className="text-sm font-bold text-gray-900">
          ₩{booking.totalPrice.toLocaleString()}
        </p>
      </div>

      {booking.status === "PENDING" && booking.holdExpiresAt && (
        <p className="mt-2 text-xs text-yellow-600">
          Hold expires:{" "}
          {new Date(booking.holdExpiresAt).toLocaleTimeString("ko-KR")}
        </p>
      )}
    </Link>
  );
}
