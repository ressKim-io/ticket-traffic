"use client";

import { useState, useEffect } from "react";
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
  const [teamInput, setTeamInput] = useState(params.teamName ?? "");

  useEffect(() => {
    const timer = setTimeout(() => {
      const trimmed = teamInput.trim() || undefined;
      if (trimmed !== params.teamName) {
        onChange({ ...params, teamName: trimmed, page: 0 });
      }
    }, 400);
    return () => clearTimeout(timer);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [teamInput]);

  return (
    <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
      {/* Status Filter */}
      <select
        aria-label="Filter games by status"
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

      {/* Team Search (debounced) */}
      <input
        type="text"
        placeholder="Search team..."
        aria-label="Search games by team name"
        value={teamInput}
        onChange={(e) => setTeamInput(e.target.value)}
        className="rounded-lg border border-gray-300 px-3 py-2 text-sm placeholder:text-gray-400 focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500"
      />
    </div>
  );
}
