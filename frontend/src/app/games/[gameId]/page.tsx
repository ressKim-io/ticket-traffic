"use client";

import { useParams } from "next/navigation";
import Link from "next/link";
import { useGameDetail } from "@/hooks";
import { StatusBadge } from "@/components/game";
import { formatDate, formatTime } from "@/lib/format";
import type { SeatGrade } from "@/types";

const gradeColors: Record<SeatGrade, string> = {
  VIP: "bg-yellow-100 text-yellow-800",
  RED: "bg-red-100 text-red-700",
  BLUE: "bg-blue-100 text-blue-700",
  GRAY: "bg-gray-100 text-gray-600",
};

export default function GameDetailPage() {
  const { gameId } = useParams<{ gameId: string }>();
  const id = Number(gameId);
  const { data, isLoading, isError } = useGameDetail(id);
  const game = data?.data;

  if (isLoading) {
    return (
      <div className="mx-auto max-w-4xl px-4 py-12 sm:px-6 lg:px-8">
        <div className="flex justify-center">
          <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary-200 border-t-primary-600" />
        </div>
      </div>
    );
  }

  if (isError || !game) {
    return (
      <div className="mx-auto max-w-4xl px-4 py-12 sm:px-6 lg:px-8">
        <div className="text-center">
          <p className="text-gray-500">Failed to load game details.</p>
          <Link
            href="/games"
            className="mt-4 inline-block text-sm font-medium text-primary-600 hover:text-primary-700"
          >
            ← Back to Games
          </Link>
        </div>
      </div>
    );
  }

  const totalAvailable = game.sections.reduce(
    (sum, s) => sum + s.availableSeats,
    0
  );
  const totalSeats = game.sections.reduce((sum, s) => sum + s.totalSeats, 0);

  return (
    <div className="mx-auto max-w-4xl px-4 py-8 sm:px-6 lg:px-8">
      {/* Back Link */}
      <Link
        href="/games"
        className="inline-flex items-center text-sm text-gray-500 hover:text-gray-700"
      >
        ← Back to Games
      </Link>

      {/* Game Info Card */}
      <div className="mt-6 rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
        <div className="flex items-start justify-between">
          <StatusBadge status={game.status} />
          <span className="text-sm text-gray-500">
            Max {game.maxTicketsPerUser} tickets per person
          </span>
        </div>

        {/* Teams */}
        <div className="mt-6 text-center">
          <div className="flex items-center justify-center gap-4">
            <span className="text-2xl font-bold text-gray-900">
              {game.homeTeam}
            </span>
            <span className="text-lg font-medium text-gray-400">vs</span>
            <span className="text-2xl font-bold text-gray-900">
              {game.awayTeam}
            </span>
          </div>
        </div>

        {/* Details Grid */}
        <div className="mt-6 grid grid-cols-2 gap-4 sm:grid-cols-4">
          <div className="rounded-lg bg-gray-50 p-3 text-center">
            <p className="text-xs text-gray-500">Date</p>
            <p className="mt-1 text-sm font-medium text-gray-900">
              {formatDate(game.gameDate)}
            </p>
          </div>
          <div className="rounded-lg bg-gray-50 p-3 text-center">
            <p className="text-xs text-gray-500">Time</p>
            <p className="mt-1 text-sm font-medium text-gray-900">
              {formatTime(game.gameDate)}
            </p>
          </div>
          <div className="rounded-lg bg-gray-50 p-3 text-center">
            <p className="text-xs text-gray-500">Stadium</p>
            <p className="mt-1 text-sm font-medium text-gray-900">
              {game.stadium.name}
            </p>
          </div>
          <div className="rounded-lg bg-gray-50 p-3 text-center">
            <p className="text-xs text-gray-500">Ticket Open</p>
            <p className="mt-1 text-sm font-medium text-gray-900">
              {formatDate(game.ticketOpenAt)}
            </p>
            <p className="text-xs text-gray-500">
              {formatTime(game.ticketOpenAt)}
            </p>
          </div>
        </div>

        {/* Stadium Address */}
        <p className="mt-4 text-center text-xs text-gray-400">
          {game.stadium.address}
        </p>
      </div>

      {/* Section Availability */}
      <div className="mt-8">
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-bold text-gray-900">
            Section Availability
          </h2>
          <span className="text-sm text-gray-500">
            {totalAvailable.toLocaleString()} / {totalSeats.toLocaleString()}{" "}
            seats available
          </span>
        </div>

        <div className="mt-4 overflow-hidden rounded-xl border border-gray-200">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">
                  Section
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">
                  Grade
                </th>
                <th className="px-4 py-3 text-right text-xs font-medium uppercase tracking-wider text-gray-500">
                  Available
                </th>
                <th className="px-4 py-3 text-right text-xs font-medium uppercase tracking-wider text-gray-500">
                  Total
                </th>
                <th className="hidden px-4 py-3 sm:table-cell">
                  <span className="sr-only">Progress</span>
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100 bg-white">
              {game.sections.map((section) => {
                const pct =
                  section.totalSeats > 0
                    ? (section.availableSeats / section.totalSeats) * 100
                    : 0;
                return (
                  <tr key={section.sectionId} className="hover:bg-gray-50">
                    <td className="whitespace-nowrap px-4 py-3 text-sm font-medium text-gray-900">
                      {section.sectionName}
                    </td>
                    <td className="whitespace-nowrap px-4 py-3">
                      <span
                        className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${gradeColors[section.grade]}`}
                      >
                        {section.grade}
                      </span>
                    </td>
                    <td className="whitespace-nowrap px-4 py-3 text-right text-sm text-gray-700">
                      {section.availableSeats.toLocaleString()}
                    </td>
                    <td className="whitespace-nowrap px-4 py-3 text-right text-sm text-gray-500">
                      {section.totalSeats.toLocaleString()}
                    </td>
                    <td className="hidden w-32 px-4 py-3 sm:table-cell">
                      <div className="h-2 w-full overflow-hidden rounded-full bg-gray-200">
                        <div
                          className={`h-full rounded-full transition-all ${
                            pct > 50
                              ? "bg-green-500"
                              : pct > 20
                                ? "bg-yellow-500"
                                : pct > 0
                                  ? "bg-red-500"
                                  : "bg-gray-300"
                          }`}
                          style={{ width: `${pct}%` }}
                        />
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>

      {/* CTA Button */}
      {game.status === "OPEN" && (
        <div className="mt-8 text-center">
          <Link
            href={`/games/${game.id}/queue`}
            className="inline-flex items-center rounded-lg bg-primary-600 px-6 py-3 text-sm font-semibold text-white shadow-sm transition-colors hover:bg-primary-700"
          >
            Join Queue to Buy Tickets
          </Link>
        </div>
      )}
    </div>
  );
}
