import Link from "next/link";
import type { GameResponse } from "@/types";
import { formatDate, formatTime } from "@/lib/format";
import { StatusBadge } from "./status-badge";

interface GameCardProps {
  game: GameResponse;
}

export function GameCard({ game }: GameCardProps) {
  return (
    <Link
      href={`/games/${game.id}`}
      className="group block rounded-xl border border-gray-200 p-5 transition-all hover:border-primary-300 hover:shadow-md"
    >
      <div className="flex items-start justify-between">
        <StatusBadge status={game.status} />
        <span className="text-xs text-gray-500">{game.stadiumName}</span>
      </div>

      <div className="mt-4 text-center">
        <div className="flex items-center justify-center gap-3">
          <span className="text-lg font-bold text-gray-900">
            {game.homeTeam}
          </span>
          <span className="text-sm font-medium text-gray-400">vs</span>
          <span className="text-lg font-bold text-gray-900">
            {game.awayTeam}
          </span>
        </div>
      </div>

      <div className="mt-4 flex items-center justify-between text-sm text-gray-600">
        <span>{formatDate(game.gameDate)}</span>
        <span>{formatTime(game.gameDate)}</span>
      </div>

      {game.status === "OPEN" && (
        <div className="mt-3 text-center">
          <span className="text-xs font-medium text-primary-600 group-hover:text-primary-700">
            Buy Tickets →
          </span>
        </div>
      )}
    </Link>
  );
}
