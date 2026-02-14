"use client";

import type { GameStatus, GameListParams } from "@/types";

interface GameFiltersProps {
  params: GameListParams;
  onChange: (params: GameListParams) => void;
}

const statuses: { value: GameStatus | ""; label: string }[] = [
  { value: "", label: "All Status" },
  { value: "OPEN", label: "On Sale" },
  { value: "SCHEDULED", label: "Scheduled" },
  { value: "SOLD_OUT", label: "Sold Out" },
  { value: "CLOSED", label: "Closed" },
];

export function GameFilters({ params, onChange }: GameFiltersProps) {
  return (
    <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
      {/* Status Filter */}
      <select
        value={params.status ?? ""}
        onChange={(e) =>
          onChange({
            ...params,
            status: (e.target.value || undefined) as GameStatus | undefined,
            page: 0,
          })
        }
        className="rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500"
      >
        {statuses.map((s) => (
          <option key={s.value} value={s.value}>
            {s.label}
          </option>
        ))}
      </select>

      {/* Team Search */}
      <input
        type="text"
        placeholder="Search team..."
        value={params.teamName ?? ""}
        onChange={(e) =>
          onChange({ ...params, teamName: e.target.value || undefined, page: 0 })
        }
        className="rounded-lg border border-gray-300 px-3 py-2 text-sm placeholder:text-gray-400 focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500"
      />
    </div>
  );
}
