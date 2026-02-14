"use client";

import { useState, useCallback, useEffect } from "react";
import { useParams, useRouter } from "next/navigation";
import Link from "next/link";
import { useQueryClient } from "@tanstack/react-query";
import { AuthGuard } from "@/components/auth";
import { CountdownTimer } from "@/components/payment";
import { useBooking, useConfirmBooking, useCancelBooking } from "@/hooks";
import { getErrorMessage } from "@/lib";

function PaymentContent() {
  const { bookingId } = useParams<{ bookingId: string }>();
  const router = useRouter();
  const queryClient = useQueryClient();
  const id = Number(bookingId);
  const isValidId = !!bookingId && !isNaN(id) && id > 0;

  const [expired, setExpired] = useState(false);

  const { data, isLoading, isError, error } = useBooking(isValidId ? id : 0, {
    enabled: !expired,
  });
  const booking = data?.data;

  const confirmMutation = useConfirmBooking();
  const cancelMutation = useCancelBooking();

  useEffect(() => {
    document.title = "Complete Payment | SportsTix";
  }, []);

  const handleConfirm = () => {
    if (!isValidId || confirmMutation.isPending) return;
    confirmMutation.mutate(id);
  };

  const handleCancel = () => {
    if (!isValidId || cancelMutation.isPending) return;
    cancelMutation.mutate(id, {
      onSuccess: (res) => {
        if (res.success) {
          router.push("/games");
        }
      },
    });
  };

  const handleExpire = useCallback(() => {
    setExpired(true);
    queryClient.cancelQueries({ queryKey: ["booking", id] });
  }, [id, queryClient]);

  if (!isValidId) {
    return (
      <div className="py-12 text-center">
        <p className="text-gray-500">Invalid booking ID.</p>
        <Link
          href="/games"
          className="mt-4 inline-block text-sm font-medium text-primary-600 hover:text-primary-700"
        >
          ← Back to Games
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
          href="/games"
          className="mt-4 inline-block text-sm font-medium text-primary-600 hover:text-primary-700"
        >
          ← Back to Games
        </Link>
      </div>
    );
  }

  // CONFIRMED state
  if (booking.status === "CONFIRMED") {
    return (
      <div className="mx-auto max-w-md text-center">
        <div className="rounded-xl border-2 border-green-300 bg-green-50 p-8">
          <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-green-100">
            <svg
              className="h-8 w-8 text-green-600"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M5 13l4 4L19 7"
              />
            </svg>
          </div>
          <h2 className="mt-4 text-xl font-bold text-green-800">
            Booking Confirmed!
          </h2>
          <p className="mt-2 text-sm text-green-600">
            Your tickets have been secured. Booking #{booking.bookingId}
          </p>
          <p className="mt-1 text-lg font-bold text-green-800">
            ₩{booking.totalPrice.toLocaleString()}
          </p>
          <div className="mt-4 space-y-1 text-sm text-green-700">
            {booking.seats.map((seat) => (
              <p key={seat.gameSeatId}>
                Seat #{seat.gameSeatId} — ₩{seat.price.toLocaleString()}
              </p>
            ))}
          </div>
          <Link
            href="/games"
            className="mt-6 inline-block rounded-lg bg-green-600 px-6 py-2.5 text-sm font-semibold text-white hover:bg-green-700"
          >
            Back to Games
          </Link>
        </div>
      </div>
    );
  }

  // CANCELLED state
  if (booking.status === "CANCELLED") {
    return (
      <div className="mx-auto max-w-md text-center">
        <div className="rounded-xl border border-gray-300 bg-gray-50 p-8">
          <h2 className="text-xl font-bold text-gray-700">Booking Cancelled</h2>
          <p className="mt-2 text-sm text-gray-500">
            This booking has been cancelled. Your seats have been released.
          </p>
          <Link
            href="/games"
            className="mt-6 inline-block rounded-lg bg-primary-600 px-6 py-2.5 text-sm font-semibold text-white hover:bg-primary-700"
          >
            Browse Games
          </Link>
        </div>
      </div>
    );
  }

  // PENDING state (payment)
  return (
    <div className="mx-auto max-w-lg">
      <h1 className="text-xl font-bold text-gray-900">Complete Your Payment</h1>
      <p className="mt-1 text-sm text-gray-500">
        Booking #{booking.bookingId}
      </p>

      {/* Countdown Timer */}
      <div className="mt-6">
        {!expired && booking.holdExpiresAt ? (
          <CountdownTimer
            expiresAt={booking.holdExpiresAt}
            onExpire={handleExpire}
          />
        ) : (
          <div className="rounded-lg border border-red-200 bg-red-50 p-4 text-center">
            <p className="font-medium text-red-800">Hold Expired</p>
            <p className="mt-1 text-sm text-red-600">
              Your seat hold has expired. Please try again.
            </p>
            <Link
              href="/games"
              className="mt-3 inline-block text-sm font-medium text-primary-600 hover:text-primary-700"
            >
              ← Back to Games
            </Link>
          </div>
        )}
      </div>

      {/* Order Summary */}
      {!expired && (
        <>
          <div className="mt-6 rounded-xl border border-gray-200 bg-white p-5">
            <h2 className="text-sm font-bold text-gray-900">Order Summary</h2>
            <div className="mt-3 divide-y divide-gray-100">
              {booking.seats.map((seat) => (
                <div
                  key={seat.gameSeatId}
                  className="flex items-center justify-between py-2"
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

          {/* Action Buttons */}
          <div className="mt-6 space-y-3">
            <button
              onClick={handleConfirm}
              disabled={confirmMutation.isPending || cancelMutation.isPending}
              className="w-full rounded-lg bg-primary-600 px-4 py-3 text-sm font-semibold text-white shadow-sm transition-colors hover:bg-primary-700 disabled:cursor-not-allowed disabled:opacity-50"
            >
              {confirmMutation.isPending ? "Processing Payment..." : "Confirm & Pay"}
            </button>

            <button
              onClick={handleCancel}
              disabled={confirmMutation.isPending || cancelMutation.isPending}
              className="w-full rounded-lg border border-gray-300 px-4 py-3 text-sm font-medium text-gray-700 transition-colors hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-50"
            >
              {cancelMutation.isPending ? "Cancelling..." : "Cancel Booking"}
            </button>
          </div>

          {/* Error messages */}
          {confirmMutation.isError && (
            <div className="mt-3 rounded-lg border border-red-200 bg-red-50 p-3">
              <p className="text-sm font-medium text-red-800">Payment failed</p>
              <p className="mt-1 text-xs text-red-600">
                {getErrorMessage(confirmMutation.error)}
              </p>
            </div>
          )}

          {cancelMutation.isError && (
            <div className="mt-3 rounded-lg border border-red-200 bg-red-50 p-3">
              <p className="text-sm font-medium text-red-800">Cancel failed</p>
              <p className="mt-1 text-xs text-red-600">
                {getErrorMessage(cancelMutation.error)}
              </p>
            </div>
          )}
        </>
      )}
    </div>
  );
}

export default function PaymentPage() {
  return (
    <div className="mx-auto max-w-4xl px-4 py-8 sm:px-6 lg:px-8">
      <AuthGuard>
        <PaymentContent />
      </AuthGuard>
    </div>
  );
}
