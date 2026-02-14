"use client";

import { useState } from "react";
import { useGames } from "@/hooks";
import { getErrorMessage } from "@/lib";
import { GameCard, GameFilters, Pagination } from "@/components/game";
import type { GameListParams } from "@/types";

export default function GamesPage() {
  const [params, setParams] = useState<GameListParams>({
    page: 0,
    size: 12,
  });

  const { data, isLoading, isError, error } = useGames(params);
  const pageData = data?.data;

  return (
    <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <h1 className="text-2xl font-bold text-gray-900">Upcoming Games</h1>
        <GameFilters params={params} onChange={setParams} />
      </div>

      {isLoading && (
        <div className="mt-12 flex justify-center" role="status" aria-live="polite">
          <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary-200 border-t-primary-600" />
          <span className="sr-only">Loading games...</span>
        </div>
      )}

      {isError && (
        <div className="mt-12 text-center">
          <p className="font-medium text-red-800">Failed to load games.</p>
          <p className="mt-1 text-sm text-red-600">{getErrorMessage(error)}</p>
        </div>
      )}

      {pageData && pageData.content.length === 0 && (
        <div className="mt-12 text-center">
          <p className="text-gray-500">No games found.</p>
        </div>
      )}

      {pageData && pageData.content.length > 0 && (
        <>
          <div className="mt-8 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {pageData.content.map((game) => (
              <GameCard key={game.id} game={game} />
            ))}
          </div>

          <div className="mt-8">
            <Pagination
              page={pageData.number}
              totalPages={pageData.totalPages}
              onPageChange={(page) => setParams((prev) => ({ ...prev, page }))}
            />
          </div>
        </>
      )}
    </div>
  );
}
