"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuthStore } from "@/stores";

interface GuestGuardProps {
  children: React.ReactNode;
}

/**
 * Protects routes that should only be accessible to guests (not logged in).
 * Redirects to / if user is already authenticated.
 */
export function GuestGuard({ children }: GuestGuardProps) {
  const router = useRouter();
  const { isAuthenticated, hydrated } = useAuthStore();

  useEffect(() => {
    if (hydrated && isAuthenticated) {
      router.replace("/");
    }
  }, [hydrated, isAuthenticated, router]);

  if (!hydrated) {
    return (
      <div className="flex min-h-[60vh] items-center justify-center">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary-200 border-t-primary-600" />
      </div>
    );
  }

  if (isAuthenticated) return null;

  return <>{children}</>;
}
