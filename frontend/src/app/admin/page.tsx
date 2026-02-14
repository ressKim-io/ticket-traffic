"use client";

import { useEffect } from "react";
import { AuthGuard } from "@/components/auth";
import { useDashboard, useGameStats } from "@/hooks";
import { getErrorMessage } from "@/lib";

function StatCard({
  label,
  value,
  sub,
}: {
  label: string;
  value: string;
  sub?: string;
}) {
  return (
    <div className="rounded-xl border border-gray-200 bg-white p-5">
      <p className="text-xs font-medium text-gray-500">{label}</p>
      <p className="mt-1 text-2xl font-bold text-gray-900">{value}</p>
      {sub && <p className="mt-0.5 text-xs text-gray-400">{sub}</p>}
    </div>
  );
}

function DashboardContent() {
  const {
    data: dashboard,
    isLoading: dashLoading,
    isError: dashError,
    error: dashErr,
  } = useDashboard();
  const {
    data: gameStats = [],
    isLoading: gamesLoading,
    isError: gamesError,
    error: gamesErr,
  } = useGameStats();

  useEffect(() => {
    const total = dashboard?.totalBookings ?? 0;
    document.title = total > 0
      ? `(${total.toLocaleString()}) Admin Dashboard | SportsTix`
      : "Admin Dashboard | SportsTix";
  }, [dashboard?.totalBookings]);

  if (dashLoading || gamesLoading) {
    return (
      <div
        className="flex justify-center py-12"
        role="status"
        aria-live="polite"
      >
        <div
          className="h-8 w-8 animate-spin rounded-full border-4 border-primary-200 border-t-primary-600"
          aria-hidden="true"
        />
        <span className="sr-only">Loading dashboard...</span>
      </div>
    );
  }

  if (dashError || gamesError) {
    return (
      <div className="py-12 text-center">
        <p className="font-medium text-red-800">
          Failed to load dashboard data.
        </p>
        <p className="mt-1 text-sm text-red-600">
          {getErrorMessage(dashErr || gamesErr)}
        </p>
      </div>
    );
  }

  if (!dashboard) return null;

  const conversionRate =
    dashboard.totalBookings > 0
      ? ((dashboard.confirmedBookings / dashboard.totalBookings) * 100).toFixed(
          1
        )
      : "0";

  return (
    <div>
      {/* Summary Cards */}
      <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
        <StatCard
          label="Total Bookings"
          value={dashboard.totalBookings.toLocaleString()}
        />
        <StatCard
          label="Confirmed"
          value={dashboard.confirmedBookings.toLocaleString()}
          sub={`${conversionRate}% conversion`}
        />
        <StatCard
          label="Total Revenue"
          value={`₩${dashboard.totalRevenue.toLocaleString()}`}
        />
        <StatCard
          label="Total Games"
          value={dashboard.totalGames.toLocaleString()}
        />
      </div>

      {/* Game Stats Table */}
      <div className="mt-8">
        <h2 className="text-lg font-bold text-gray-900">Game Statistics</h2>
        <div className="mt-4 overflow-x-auto rounded-xl border border-gray-200">
          <table className="min-w-full divide-y divide-gray-200">
            <caption className="sr-only">
              Booking statistics per game
            </caption>
            <thead className="bg-gray-50">
              <tr>
                <th
                  scope="col"
                  className="px-4 py-3 text-left text-xs font-medium uppercase text-gray-500"
                >
                  Game
                </th>
                <th
                  scope="col"
                  className="px-4 py-3 text-right text-xs font-medium uppercase text-gray-500"
                >
                  Bookings
                </th>
                <th
                  scope="col"
                  className="px-4 py-3 text-right text-xs font-medium uppercase text-gray-500"
                >
                  Confirmed
                </th>
                <th
                  scope="col"
                  className="px-4 py-3 text-right text-xs font-medium uppercase text-gray-500"
                >
                  Cancelled
                </th>
                <th
                  scope="col"
                  className="px-4 py-3 text-right text-xs font-medium uppercase text-gray-500"
                >
                  Revenue
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100 bg-white">
              {gameStats.length === 0 ? (
                <tr>
                  <td
                    colSpan={5}
                    className="px-4 py-8 text-center text-sm text-gray-500"
                  >
                    No game statistics available yet.
                  </td>
                </tr>
              ) : (
                gameStats.map((game) => (
                  <tr key={game.gameId} className="hover:bg-gray-50">
                    <td className="whitespace-nowrap px-4 py-3">
                      <p className="text-sm font-medium text-gray-900">
                        {game.homeTeam ?? "TBD"} vs{" "}
                        {game.awayTeam ?? "TBD"}
                      </p>
                      <p className="text-xs text-gray-500">
                        Game #{game.gameId}
                      </p>
                    </td>
                    <td className="whitespace-nowrap px-4 py-3 text-right text-sm text-gray-900">
                      {game.totalBookings.toLocaleString()}
                    </td>
                    <td className="whitespace-nowrap px-4 py-3 text-right text-sm text-green-700">
                      {game.confirmedBookings.toLocaleString()}
                    </td>
                    <td className="whitespace-nowrap px-4 py-3 text-right text-sm text-red-600">
                      {game.cancelledBookings.toLocaleString()}
                    </td>
                    <td className="whitespace-nowrap px-4 py-3 text-right text-sm font-medium text-gray-900">
                      ₩{game.totalRevenue.toLocaleString()}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

export default function AdminPage() {
  return (
    <div className="mx-auto max-w-6xl px-4 py-8 sm:px-6 lg:px-8">
      <h1 className="text-2xl font-bold text-gray-900">Admin Dashboard</h1>
      <p className="mt-1 text-sm text-gray-500">
        Real-time booking and revenue statistics.
      </p>
      <div className="mt-6">
        <AuthGuard>
          <DashboardContent />
        </AuthGuard>
      </div>
    </div>
  );
}
