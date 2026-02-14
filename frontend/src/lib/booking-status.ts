import type { BookingStatus } from "@/types";

interface StatusStyle {
  label: string;
  bg: string;
  text: string;
  border: string;
  dot: string;
}

export const BOOKING_STATUS_CONFIG: Record<BookingStatus, StatusStyle> = {
  PENDING: {
    label: "Pending",
    bg: "bg-yellow-50",
    text: "text-yellow-800",
    border: "border-yellow-200",
    dot: "bg-yellow-400",
  },
  CONFIRMED: {
    label: "Confirmed",
    bg: "bg-green-50",
    text: "text-green-800",
    border: "border-green-200",
    dot: "bg-green-400",
  },
  CANCELLED: {
    label: "Cancelled",
    bg: "bg-gray-50",
    text: "text-gray-600",
    border: "border-gray-200",
    dot: "bg-gray-400",
  },
};
