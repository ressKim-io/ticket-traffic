"use client";

import { useState, useEffect } from "react";
import { useParams } from "next/navigation";
import Link from "next/link";
import { AuthGuard } from "@/components/auth";
import { useBooking, useCancelBooking } from "@/hooks";
import { getErrorMessage } from "@/lib";
import type { BookingStatus } from "@/types";

const statusConfig: Record<
  BookingStatus,
  { label: string; bg: string; text: string; border: string }
> = {
  PENDING: {
    label: "Pending Payment",
    bg: "bg-yellow-50",
    text: "text-yellow-800",
    border: "border-yellow-200",
  },
  CONFIRMED: {
    label: "Confirmed",
    bg: "bg-green-50",
    text: "text-green-800",
    border: "border-green-200",
  },
  CANCELLED: {
    label: "Cancelled",
    bg: "bg-gray-50",
    text: "text-gray-600",
    border: "border-gray-200",
  },
};

function BookingDetail() {
  const { bookingId } = useParams<{ bookingId: string }>();
  const id = Number(bookingId);
  const isValidId = !!bookingId && !isNaN(id) && id > 0;

  const [showCancelConfirm, setShowCancelConfirm] = useState(false);

  const { data, isLoading, isError, error } = useBooking(isValidId ? id : 0);
  const booking = data?.data;

  const cancelMutation = useCancelBooking();

  useEffect(() => {
    document.title = booking
      ? `Booking #${booking.bookingId} | SportsTix`
      : "Booking Detail | SportsTix";
  }, [booking]);

  const handleCancel = () => {
    if (!isValidId || cancelMutation.isPending) return;
    cancelMutation.mutate(id, {
      onSuccess: (res) => {
        if (res.success) {
          setShowCancelConfirm(false);
        }
      },
    });
  };

  if (!isValidId) {
    return (
      <div className="py-12 text-center">
        <p className="text-gray-500">Invalid booking ID.</p>
        <Link
          href="/my"
          className="mt-4 inline-block text-sm font-medium text-primary-600 hover:text-primary-700"
        >
          &larr; Back to My Bookings
        </Link>
      </div>
    );
  }

  if (isLoading) {
    return (
      <div className="flex justify-center py-12" role="status" aria-live="polite">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary-200 border-t-primary-600" />
        <span className="sr-only">Loading booking details...</span>
      </div>
    );
  }

  if (isError || !booking) {
    return (
      <div className="py-12 text-center">
        <p className="font-medium text-red-800">Failed to load booking.</p>
        <p className="mt-1 text-sm text-red-600">{getErrorMessage(error)}</p>
        <Link
          href="/my"
          className="mt-4 inline-block text-sm font-medium text-primary-600 hover:text-primary-700"
        >
          &larr; Back to My Bookings
        </Link>
      </div>
    );
  }

  const config = statusConfig[booking.status];
  const createdDate = new Date(booking.createdAt).toLocaleDateString("ko-KR", {
    year: "numeric",
    month: "long",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });

  const canCancel =
    booking.status === "PENDING" || booking.status === "CONFIRMED";

  return (
    <div>
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <Link
            href="/my"
            className="text-sm font-medium text-primary-600 hover:text-primary-700"
          >
            &larr; My Bookings
          </Link>
          <h1 className="mt-2 text-xl font-bold text-gray-900">
            Booking #{booking.bookingId}
          </h1>
          <p className="mt-0.5 text-sm text-gray-500">{createdDate}</p>
        </div>
        <span
          className={`inline-flex items-center rounded-full border px-3 py-1 text-sm font-medium ${config.bg} ${config.text} ${config.border}`}
        >
          {config.label}
        </span>
      </div>

      {/* PENDING: redirect to payment */}
      {booking.status === "PENDING" && (
        <div className="mt-4 rounded-lg border border-yellow-200 bg-yellow-50 p-4">
          <p className="text-sm font-medium text-yellow-800">
            Payment is pending for this booking.
          </p>
          {booking.holdExpiresAt && (
            <p className="mt-1 text-xs text-yellow-600">
              Hold expires:{" "}
              {new Date(booking.holdExpiresAt).toLocaleTimeString("ko-KR")}
            </p>
          )}
          <Link
            href={`/bookings/${booking.bookingId}/payment`}
            className="mt-3 inline-block rounded-lg bg-yellow-600 px-4 py-2 text-sm font-semibold text-white hover:bg-yellow-700"
          >
            Complete Payment
          </Link>
        </div>
      )}

      {/* Seat Details */}
      <div className="mt-6 rounded-xl border border-gray-200 bg-white p-5">
        <h2 className="text-sm font-bold text-gray-900">Seats</h2>
        <div className="mt-3 divide-y divide-gray-100">
          {booking.seats.map((seat) => (
            <div
              key={seat.gameSeatId}
              className="flex items-center justify-between py-2.5"
            >
              <span className="text-sm text-gray-700">
                Seat #{seat.gameSeatId}
              </span>
              <span className="text-sm font-medium text-gray-900">
                ₩{seat.price.toLocaleString()}
              </span>
            </div>
          ))}
        </div>
        <div className="mt-3 flex items-center justify-between border-t border-gray-200 pt-3">
          <span className="text-sm font-bold text-gray-900">Total</span>
          <span className="text-lg font-bold text-primary-700">
            ₩{booking.totalPrice.toLocaleString()}
          </span>
        </div>
      </div>

      {/* Cancel Section */}
      {canCancel && (
        <div className="mt-6">
          {!showCancelConfirm ? (
            <button
              onClick={() => setShowCancelConfirm(true)}
              className="w-full rounded-lg border border-red-300 px-4 py-2.5 text-sm font-medium text-red-700 transition-colors hover:bg-red-50"
            >
              Cancel Booking
            </button>
          ) : (
            <div className="rounded-lg border border-red-200 bg-red-50 p-4">
              <p className="text-sm font-medium text-red-800">
                Are you sure you want to cancel this booking?
              </p>
              <p className="mt-1 text-xs text-red-600">
                {booking.status === "CONFIRMED"
                  ? "A refund will be processed for your payment."
                  : "Your held seats will be released."}
              </p>
              <div className="mt-3 flex gap-3">
                <button
                  onClick={handleCancel}
                  disabled={cancelMutation.isPending}
                  className="rounded-lg bg-red-600 px-4 py-2 text-sm font-semibold text-white hover:bg-red-700 disabled:cursor-not-allowed disabled:opacity-50"
                >
                  {cancelMutation.isPending
                    ? "Cancelling..."
                    : "Yes, Cancel"}
                </button>
                <button
                  onClick={() => setShowCancelConfirm(false)}
                  disabled={cancelMutation.isPending}
                  className="rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-50"
                >
                  Keep Booking
                </button>
              </div>

              {cancelMutation.isError && (
                <p className="mt-2 text-xs text-red-600">
                  {getErrorMessage(cancelMutation.error)}
                </p>
              )}
            </div>
          )}
        </div>
      )}
    </div>
  );
}

export default function BookingDetailPage() {
  return (
    <div className="mx-auto max-w-2xl px-4 py-8 sm:px-6 lg:px-8">
      <AuthGuard>
        <BookingDetail />
      </AuthGuard>
    </div>
  );
}
