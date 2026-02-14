"use client";

import { useState, useEffect } from "react";

interface CountdownTimerProps {
  expiresAt: string;
  onExpire: () => void;
}

export function CountdownTimer({ expiresAt, onExpire }: CountdownTimerProps) {
  const [remaining, setRemaining] = useState(() => calcRemaining(expiresAt));

  useEffect(() => {
    const interval = setInterval(() => {
      const secs = calcRemaining(expiresAt);
      setRemaining(secs);
      if (secs <= 0) {
        clearInterval(interval);
        onExpire();
      }
    }, 1000);

    return () => clearInterval(interval);
  }, [expiresAt, onExpire]);

  const minutes = Math.floor(Math.max(0, remaining) / 60);
  const seconds = Math.max(0, remaining) % 60;
  const isUrgent = remaining <= 60;

  return (
    <div
      className={`rounded-lg p-4 text-center ${
        isUrgent ? "bg-red-50 border border-red-200" : "bg-gray-50"
      }`}
      role="timer"
      aria-live="polite"
    >
      <p className={`text-xs ${isUrgent ? "text-red-600" : "text-gray-500"}`}>
        Time Remaining
      </p>
      <p
        className={`mt-1 text-3xl font-bold tabular-nums ${
          isUrgent ? "text-red-700" : "text-gray-900"
        }`}
      >
        {String(minutes).padStart(2, "0")}:{String(seconds).padStart(2, "0")}
      </p>
      {isUrgent && (
        <p className="mt-1 text-xs text-red-500">
          Hurry! Your hold will expire soon.
        </p>
      )}
    </div>
  );
}

function calcRemaining(expiresAt: string): number {
  return Math.floor((new Date(expiresAt).getTime() - Date.now()) / 1000);
}
