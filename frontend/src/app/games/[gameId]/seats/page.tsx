"use client";

import { useState, useCallback } from "react";
import { useParams, useRouter } from "next/navigation";
import Link from "next/link";
import { AuthGuard } from "@/components/auth";
import { SectionSelector, SeatGrid, SelectedSeatsPanel } from "@/components/seat";
import { useGameDetail, useGameSeats, useHoldSeats } from "@/hooks";
import { getErrorMessage } from "@/lib";
import type { GameSeatResponse } from "@/types";

const MAX_SEATS = 4;

function SeatsContent() {
  const { gameId } = useParams<{ gameId: string }>();
  const router = useRouter();
  const id = Number(gameId);
  const isValidId = !!gameId && !isNaN(id) && id > 0;

  const [selectedSectionId, setSelectedSectionId] = useState<number | null>(null);
  const [selectedSeats, setSelectedSeats] = useState<Map<number, GameSeatResponse>>(new Map());

  const { data: gameData, isLoading: gameLoading } = useGameDetail(isValidId ? id : 0);
  const game = gameData?.data;

  const { data: seatsData, isLoading: seatsLoading } = useGameSeats(
    isValidId ? id : 0,
    selectedSectionId
  );
  const seats = seatsData?.data ?? [];

  const holdMutation = useHoldSeats();

  // Verify queue token exists
  const hasToken =
    typeof window !== "undefined" &&
    !!sessionStorage.getItem(`queue-token-${id}`);

  const handleSectionSelect = useCallback((sectionId: number) => {
    setSelectedSectionId(sectionId);
    setSelectedSeats(new Map()); // Clear selections when switching sections
  }, []);

  const handleSeatToggle = useCallback((seat: GameSeatResponse) => {
    setSelectedSeats((prev) => {
      const next = new Map(prev);
      if (next.has(seat.gameSeatId)) {
        next.delete(seat.gameSeatId);
      } else if (next.size < MAX_SEATS) {
        next.set(seat.gameSeatId, seat);
      }
      return next;
    });
  }, []);

  const handleRemoveSeat = useCallback((gameSeatId: number) => {
    setSelectedSeats((prev) => {
      const next = new Map(prev);
      next.delete(gameSeatId);
      return next;
    });
  }, []);

  const handleHold = useCallback(() => {
    if (!isValidId || selectedSeats.size === 0) return;

    holdMutation.mutate(
      { gameId: id, gameSeatIds: Array.from(selectedSeats.keys()) },
      {
        onSuccess: (res) => {
          if (res.success && res.data) {
            // Store booking info and navigate to payment
            sessionStorage.setItem(
              `booking-${res.data.bookingId}`,
              JSON.stringify(res.data)
            );
            router.push(`/bookings/${res.data.bookingId}/payment`);
          }
        },
      }
    );
  }, [id, isValidId, selectedSeats, holdMutation, router]);

  if (!isValidId) {
    return (
      <div className="py-12 text-center">
        <p className="text-gray-500">Invalid game ID.</p>
        <Link
          href="/games"
          className="mt-4 inline-block text-sm font-medium text-primary-600 hover:text-primary-700"
        >
          ← Back to Games
        </Link>
      </div>
    );
  }

  if (!hasToken && typeof window !== "undefined") {
    return (
      <div className="py-12 text-center">
        <p className="font-medium text-red-800">No queue token found.</p>
        <p className="mt-1 text-sm text-gray-500">
          You need to go through the queue first.
        </p>
        <Link
          href={`/games/${id}/queue`}
          className="mt-4 inline-block rounded-lg bg-primary-600 px-4 py-2 text-sm font-medium text-white hover:bg-primary-700"
        >
          Join Queue
        </Link>
      </div>
    );
  }

  if (gameLoading) {
    return (
      <div className="flex justify-center py-12" role="status" aria-live="polite">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary-200 border-t-primary-600" />
        <span className="sr-only">Loading seat map...</span>
      </div>
    );
  }

  if (!game) {
    return (
      <div className="py-12 text-center">
        <p className="text-gray-500">Game not found.</p>
        <Link
          href="/games"
          className="mt-4 inline-block text-sm font-medium text-primary-600 hover:text-primary-700"
        >
          ← Back to Games
        </Link>
      </div>
    );
  }

  const selectedSeatsList = Array.from(selectedSeats.values());
  const selectedIds = new Set(selectedSeats.keys());

  return (
    <div className="flex flex-col gap-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <Link
            href={`/games/${id}`}
            className="text-sm text-gray-500 hover:text-gray-700"
          >
            ← Back to Game
          </Link>
          <h1 className="mt-1 text-xl font-bold text-gray-900">
            {game.homeTeam} vs {game.awayTeam} — Select Seats
          </h1>
          <p className="text-sm text-gray-500">
            {game.stadium.name} &middot; Max {game.maxTicketsPerUser} seats
          </p>
        </div>
      </div>

      {/* Section Selector */}
      <div>
        <h2 className="mb-3 text-sm font-semibold text-gray-700">
          Choose a Section
        </h2>
        <SectionSelector
          sections={game.sections}
          selectedId={selectedSectionId}
          onSelect={handleSectionSelect}
        />
      </div>

      {/* Main Content */}
      {selectedSectionId && (
        <div className="grid gap-6 lg:grid-cols-3">
          {/* Seat Map */}
          <div className="lg:col-span-2">
            <div className="rounded-xl border border-gray-200 bg-white p-6">
              {seatsLoading ? (
                <div className="flex justify-center py-12" role="status">
                  <div className="h-6 w-6 animate-spin rounded-full border-4 border-primary-200 border-t-primary-600" />
                  <span className="sr-only">Loading seats...</span>
                </div>
              ) : seats.length === 0 ? (
                <p className="py-12 text-center text-gray-500">
                  No seats found for this section.
                </p>
              ) : (
                <SeatGrid
                  seats={seats}
                  selectedIds={selectedIds}
                  maxSelect={game.maxTicketsPerUser}
                  onToggle={handleSeatToggle}
                />
              )}
            </div>
          </div>

          {/* Selection Panel */}
          <div>
            <SelectedSeatsPanel
              seats={selectedSeatsList}
              maxSelect={game.maxTicketsPerUser}
              onRemove={handleRemoveSeat}
              onHold={handleHold}
              isHolding={holdMutation.isPending}
            />

            {holdMutation.isError && (
              <div className="mt-3 rounded-lg border border-red-200 bg-red-50 p-3">
                <p className="text-sm font-medium text-red-800">
                  Failed to hold seats
                </p>
                <p className="mt-1 text-xs text-red-600">
                  {getErrorMessage(holdMutation.error)}
                </p>
              </div>
            )}
          </div>
        </div>
      )}

      {!selectedSectionId && (
        <div className="rounded-xl border border-dashed border-gray-300 py-16 text-center">
          <p className="text-gray-400">
            Select a section above to view available seats.
          </p>
        </div>
      )}
    </div>
  );
}

export default function SeatsPage() {
  return (
    <div className="mx-auto max-w-6xl px-4 py-8 sm:px-6 lg:px-8">
      <AuthGuard>
        <SeatsContent />
      </AuthGuard>
    </div>
  );
}
