"use client";

import { useMemo } from "react";
import type { GameSeatResponse, SeatStatus } from "@/types";

const statusStyles: Record<SeatStatus, string> = {
  AVAILABLE:
    "bg-green-100 border-green-400 text-green-800 hover:bg-green-200 cursor-pointer",
  HELD: "bg-orange-100 border-orange-300 text-orange-600 cursor-not-allowed",
  RESERVED: "bg-gray-200 border-gray-300 text-gray-400 cursor-not-allowed",
};

interface SeatGridProps {
  seats: GameSeatResponse[];
  selectedIds: Set<number>;
  maxSelect: number;
  onToggle: (seat: GameSeatResponse) => void;
}

export function SeatGrid({
  seats,
  selectedIds,
  maxSelect,
  onToggle,
}: SeatGridProps) {
  // Group seats by row
  const rows = useMemo(() => {
    const map = new Map<string, GameSeatResponse[]>();
    for (const seat of seats) {
      const group = map.get(seat.rowNumber) ?? [];
      group.push(seat);
      map.set(seat.rowNumber, group);
    }
    // Sort seats within each row by seat number
    for (const group of map.values()) {
      group.sort((a, b) => a.seatNumber - b.seatNumber);
    }
    // Sort rows naturally
    return Array.from(map.entries()).sort((a, b) =>
      a[0].localeCompare(b[0], undefined, { numeric: true })
    );
  }, [seats]);

  const atMax = selectedIds.size >= maxSelect;

  return (
    <div className="overflow-x-auto">
      {/* Stage Indicator */}
      <div className="mb-6 flex justify-center">
        <div className="rounded-t-xl bg-gray-800 px-12 py-2 text-xs font-medium tracking-wider text-white">
          STAGE
        </div>
      </div>

      {/* Seat Grid */}
      <div className="flex flex-col items-center gap-1.5">
        {rows.map(([rowNum, rowSeats]) => (
          <div key={rowNum} className="flex items-center gap-1">
            <span className="w-6 text-right text-xs font-medium text-gray-400">
              {rowNum}
            </span>
            <div className="flex gap-1">
              {rowSeats.map((seat) => {
                const isSelected = selectedIds.has(seat.gameSeatId);
                const isAvailable = seat.status === "AVAILABLE";
                const canSelect = isAvailable && (!atMax || isSelected);

                return (
                  <button
                    key={seat.gameSeatId}
                    onClick={() => canSelect && onToggle(seat)}
                    disabled={!canSelect}
                    title={`Row ${seat.rowNumber}, Seat ${seat.seatNumber} - ₩${seat.price.toLocaleString()}`}
                    aria-label={`Row ${seat.rowNumber} Seat ${seat.seatNumber} ${seat.status === "AVAILABLE" ? (isSelected ? "selected" : "available") : seat.status.toLowerCase()}`}
                    className={`flex h-7 w-7 items-center justify-center rounded text-[10px] font-medium border transition-all ${
                      isSelected
                        ? "bg-primary-600 border-primary-700 text-white ring-2 ring-primary-300"
                        : statusStyles[seat.status]
                    } ${!canSelect ? "opacity-60" : ""}`}
                  >
                    {seat.seatNumber}
                  </button>
                );
              })}
            </div>
          </div>
        ))}
      </div>

      {/* Legend */}
      <div className="mt-6 flex items-center justify-center gap-4 text-xs">
        <div className="flex items-center gap-1.5">
          <div className="h-4 w-4 rounded border border-green-400 bg-green-100" />
          <span className="text-gray-600">Available</span>
        </div>
        <div className="flex items-center gap-1.5">
          <div className="h-4 w-4 rounded border border-primary-700 bg-primary-600" />
          <span className="text-gray-600">Selected</span>
        </div>
        <div className="flex items-center gap-1.5">
          <div className="h-4 w-4 rounded border border-orange-300 bg-orange-100" />
          <span className="text-gray-600">Held</span>
        </div>
        <div className="flex items-center gap-1.5">
          <div className="h-4 w-4 rounded border border-gray-300 bg-gray-200" />
          <span className="text-gray-600">Reserved</span>
        </div>
      </div>
    </div>
  );
}
