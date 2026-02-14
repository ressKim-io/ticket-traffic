"use client";

import { useState, useEffect, useRef } from "react";

interface CountdownTimerProps {
  expiresAt: string;
  onExpire: () => void;
}

export function CountdownTimer({ expiresAt, onExpire }: CountdownTimerProps) {
  const [remaining, setRemaining] = useState(() => calcRemaining(expiresAt));
  const onExpireRef = useRef(onExpire);
  const hasFiredRef = useRef(false);
  const [announcedUrgent, setAnnouncedUrgent] = useState(false);

  useEffect(() => {
    onExpireRef.current = onExpire;
  }, [onExpire]);

  useEffect(() => {
    hasFiredRef.current = false;

    const interval = setInterval(() => {
      const secs = calcRemaining(expiresAt);
      setRemaining(secs);
      if (secs <= 0) {
        clearInterval(interval);
        if (!hasFiredRef.current) {
          hasFiredRef.current = true;
          onExpireRef.current();
        }
      }
    }, 1000);

    return () => clearInterval(interval);
  }, [expiresAt]);

  const minutes = Math.floor(Math.max(0, remaining) / 60);
  const seconds = Math.max(0, remaining) % 60;
  const isUrgent = remaining <= 60;

  useEffect(() => {
    if (isUrgent && !announcedUrgent) {
      setAnnouncedUrgent(true);
    }
  }, [isUrgent, announcedUrgent]);

  return (
    <>
      <div
        className={`rounded-lg p-4 text-center ${
          isUrgent ? "bg-red-50 border border-red-200" : "bg-gray-50"
        }`}
        role="timer"
        aria-label={`${minutes} minutes and ${seconds} seconds remaining`}
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
      {announcedUrgent && (
        <div className="sr-only" role="alert">
          Less than one minute remaining to complete your payment.
        </div>
      )}
    </>
  );
}

function calcRemaining(expiresAt: string): number {
  return Math.floor((new Date(expiresAt).getTime() - Date.now()) / 1000);
}
