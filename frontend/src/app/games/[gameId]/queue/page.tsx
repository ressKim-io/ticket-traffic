"use client";

import { useEffect, useCallback } from "react";
import { useParams, useRouter } from "next/navigation";
import Link from "next/link";
import { AuthGuard } from "@/components/auth";
import { useQueueEnter, useQueueLeave, useQueueWebSocket } from "@/hooks";
import { useQueueStore } from "@/stores";
import { getErrorMessage } from "@/lib";

function QueueContent() {
  const { gameId } = useParams<{ gameId: string }>();
  const router = useRouter();
  const id = Number(gameId);
  const isValidId = !!gameId && !isNaN(id) && id > 0;

  const { status, rank, totalWaiting, estimatedWaitSeconds, token, connected } =
    useQueueStore();
  const reset = useQueueStore((s) => s.reset);
  const enterMutation = useQueueEnter();
  const leaveMutation = useQueueLeave();

  // Connect WebSocket
  useQueueWebSocket(isValidId ? id : 0);

  // Enter queue on mount
  useEffect(() => {
    if (isValidId && !status) {
      enterMutation.mutate(id);
    }
    return () => {
      reset();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id, isValidId]);

  // Auto-redirect when token is received (store token in sessionStorage, not URL)
  useEffect(() => {
    if (status === "ELIGIBLE" && token) {
      sessionStorage.setItem(`queue-token-${id}`, token);
      const timer = setTimeout(() => {
        router.push(`/games/${id}/seats`);
      }, 1500);
      return () => clearTimeout(timer);
    }
  }, [status, token, id, router]);

  const handleLeave = useCallback(() => {
    if (isValidId) {
      leaveMutation.mutate(id, {
        onSuccess: () => router.push(`/games/${id}`),
      });
    }
  }, [id, isValidId, leaveMutation, router]);

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

  // Error entering queue
  if (enterMutation.isError) {
    return (
      <div className="py-12 text-center">
        <div className="mx-auto max-w-md rounded-xl border border-red-200 bg-red-50 p-6">
          <p className="font-medium text-red-800">Failed to join queue</p>
          <p className="mt-1 text-sm text-red-600">
            {getErrorMessage(enterMutation.error)}
          </p>
          <div className="mt-4 flex justify-center gap-3">
            <button
              onClick={() => enterMutation.mutate(id)}
              className="rounded-lg bg-primary-600 px-4 py-2 text-sm font-medium text-white hover:bg-primary-700"
            >
              Retry
            </button>
            <Link
              href={`/games/${id}`}
              className="rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
            >
              Back to Game
            </Link>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="flex flex-col items-center py-12">
      {/* Connection Indicator */}
      <div className="mb-6 flex items-center gap-2">
        <span
          className={`h-2 w-2 rounded-full ${connected ? "bg-green-500" : "bg-yellow-500"}`}
        />
        <span className="text-xs text-gray-500">
          {connected ? "Connected" : "Connecting..."}
        </span>
      </div>

      {/* ELIGIBLE State */}
      {status === "ELIGIBLE" && token && (
        <div className="mx-auto max-w-md text-center">
          <div className="rounded-xl border-2 border-green-300 bg-green-50 p-8">
            <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-green-100">
              <svg
                className="h-8 w-8 text-green-600"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M5 13l4 4L19 7"
                />
              </svg>
            </div>
            <h2 className="mt-4 text-xl font-bold text-green-800">
              It&apos;s Your Turn!
            </h2>
            <p className="mt-2 text-sm text-green-600">
              Redirecting to seat selection...
            </p>
            <div className="mt-4">
              <div className="h-1 overflow-hidden rounded-full bg-green-200">
                <div className="h-full animate-pulse rounded-full bg-green-500" />
              </div>
            </div>
          </div>
        </div>
      )}

      {/* WAITING State */}
      {(status === "WAITING" || enterMutation.isPending) && (
        <div className="mx-auto w-full max-w-md">
          <div className="rounded-xl border border-gray-200 bg-white p-8 shadow-sm">
            {/* Pulse Animation */}
            <div className="flex justify-center">
              <div className="relative">
                <div className="h-24 w-24 animate-pulse rounded-full bg-primary-100" />
                <div className="absolute inset-0 flex items-center justify-center">
                  <span className="text-2xl font-bold text-primary-700">
                    {rank != null ? `#${rank}` : "..."}
                  </span>
                </div>
              </div>
            </div>

            <h2 className="mt-6 text-center text-lg font-bold text-gray-900">
              Waiting in Queue
            </h2>

            {/* Stats */}
            <div className="mt-6 grid grid-cols-2 gap-4" aria-live="polite" aria-atomic="true">
              <div className="rounded-lg bg-gray-50 p-3 text-center">
                <p className="text-xs text-gray-500">Your Position</p>
                <p className="mt-1 text-lg font-bold text-primary-700">
                  {rank != null ? rank.toLocaleString() : "-"}
                </p>
              </div>
              <div className="rounded-lg bg-gray-50 p-3 text-center">
                <p className="text-xs text-gray-500">People Ahead</p>
                <p className="mt-1 text-lg font-bold text-gray-900">
                  {totalWaiting != null ? totalWaiting.toLocaleString() : "-"}
                </p>
              </div>
            </div>

            {/* Estimated Wait */}
            {estimatedWaitSeconds != null && estimatedWaitSeconds > 0 && (
              <div className="mt-4 rounded-lg bg-blue-50 p-3 text-center">
                <p className="text-xs text-blue-600">Estimated Wait</p>
                <p className="mt-1 text-sm font-medium text-blue-800">
                  {formatWaitTime(estimatedWaitSeconds)}
                </p>
              </div>
            )}

            {/* Info */}
            <p className="mt-6 text-center text-xs text-gray-400">
              Please stay on this page. You will be automatically redirected
              when it&apos;s your turn.
            </p>

            {/* Leave Button */}
            <div className="mt-6 text-center">
              <button
                onClick={handleLeave}
                disabled={leaveMutation.isPending}
                className="text-sm font-medium text-gray-500 underline hover:text-gray-700 disabled:opacity-50"
              >
                {leaveMutation.isPending ? "Leaving..." : "Leave Queue"}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Initial loading (no status yet) */}
      {!status && !enterMutation.isPending && !enterMutation.isError && (
        <div className="flex justify-center" role="status" aria-live="polite">
          <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary-200 border-t-primary-600" />
          <span className="sr-only">Loading queue status...</span>
        </div>
      )}
    </div>
  );
}

function formatWaitTime(seconds: number): string {
  if (seconds < 60) return `About ${seconds} seconds`;
  const minutes = Math.ceil(seconds / 60);
  if (minutes === 1) return "About 1 minute";
  return `About ${minutes} minutes`;
}

export default function QueuePage() {
  return (
    <div className="mx-auto max-w-4xl px-4 sm:px-6 lg:px-8">
      <AuthGuard>
        <QueueContent />
      </AuthGuard>
    </div>
  );
}
