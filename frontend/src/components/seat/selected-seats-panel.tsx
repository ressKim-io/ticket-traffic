import type { GameSeatResponse } from "@/types";

interface SelectedSeatsPanelProps {
  seats: GameSeatResponse[];
  maxSelect: number;
  onRemove: (gameSeatId: number) => void;
  onHold: () => void;
  isHolding: boolean;
}

export function SelectedSeatsPanel({
  seats,
  maxSelect,
  onRemove,
  onHold,
  isHolding,
}: SelectedSeatsPanelProps) {
  const totalPrice = seats.reduce((sum, s) => sum + s.price, 0);

  return (
    <div className="rounded-xl border border-gray-200 bg-white p-5 shadow-sm">
      <div className="flex items-center justify-between">
        <h3 className="text-sm font-bold text-gray-900">
          Selected Seats ({seats.length}/{maxSelect})
        </h3>
        {seats.length > 0 && (
          <span className="text-sm font-bold text-primary-700">
            ₩{totalPrice.toLocaleString()}
          </span>
        )}
      </div>

      {seats.length === 0 ? (
        <p className="mt-3 text-sm text-gray-400">
          Click on available seats to select them.
        </p>
      ) : (
        <div className="mt-3 space-y-2">
          {seats.map((seat) => (
            <div
              key={seat.gameSeatId}
              className="flex items-center justify-between rounded-lg bg-gray-50 px-3 py-2"
            >
              <div>
                <span className="text-sm font-medium text-gray-900">
                  {seat.sectionName} - Row {seat.rowNumber}, Seat{" "}
                  {seat.seatNumber}
                </span>
                <span className="ml-2 text-xs text-gray-500">
                  ({seat.grade})
                </span>
              </div>
              <div className="flex items-center gap-3">
                <span className="text-sm font-medium text-gray-700">
                  ₩{seat.price.toLocaleString()}
                </span>
                <button
                  onClick={() => onRemove(seat.gameSeatId)}
                  aria-label={`Remove seat Row ${seat.rowNumber} Seat ${seat.seatNumber}`}
                  className="text-gray-400 hover:text-red-500"
                >
                  <svg className="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                  </svg>
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {seats.length > 0 && (
        <button
          onClick={onHold}
          disabled={isHolding}
          className="mt-4 w-full rounded-lg bg-primary-600 px-4 py-2.5 text-sm font-semibold text-white shadow-sm transition-colors hover:bg-primary-700 disabled:cursor-not-allowed disabled:opacity-50"
        >
          {isHolding ? "Processing..." : `Hold ${seats.length} Seat${seats.length > 1 ? "s" : ""} & Proceed to Payment`}
        </button>
      )}
    </div>
  );
}
