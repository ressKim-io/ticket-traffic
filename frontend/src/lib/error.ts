import axios from "axios";
import type { ApiError } from "@/types";

/**
 * Extract error message from API response or generic error.
 * Uses axios.isAxiosError type guard for safe access.
 */
export function getErrorMessage(
  error: unknown,
  fallback = "An unexpected error occurred."
): string {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data;
    if (
      data &&
      typeof data === "object" &&
      "error" in data &&
      data.error &&
      typeof data.error === "object" &&
      "message" in data.error
    ) {
      return (data.error as ApiError).message || fallback;
    }
    return error.message || fallback;
  }
  if (error instanceof Error) return error.message;
  return fallback;
}
