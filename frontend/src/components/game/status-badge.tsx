import type { GameStatus } from "@/types";

const statusConfig: Record<
  GameStatus,
  { label: string; className: string }
> = {
  SCHEDULED: {
    label: "Scheduled",
    className: "bg-gray-100 text-gray-700",
  },
  OPEN: {
    label: "On Sale",
    className: "bg-green-100 text-green-700",
  },
  SOLD_OUT: {
    label: "Sold Out",
    className: "bg-red-100 text-red-700",
  },
  CLOSED: {
    label: "Closed",
    className: "bg-gray-100 text-gray-500",
  },
};

export function StatusBadge({ status }: { status: GameStatus }) {
  const config = statusConfig[status];
  return (
    <span
      className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${config.className}`}
    >
      {config.label}
    </span>
  );
}
