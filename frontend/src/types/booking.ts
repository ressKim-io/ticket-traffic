export type SeatStatus = "AVAILABLE" | "HELD" | "RESERVED";
export type BookingStatus = "PENDING" | "CONFIRMED" | "CANCELLED";

export interface GameSeatResponse {
  gameSeatId: number;
  seatId: number;
  rowNumber: string;
  seatNumber: number;
  sectionName: string;
  grade: string;
  price: number;
  status: SeatStatus;
}

export interface HoldSeatsRequest {
  gameId: number;
  gameSeatIds: number[];
}

export interface BookingResponse {
  bookingId: number;
  userId: number;
  gameId: number;
  status: BookingStatus;
  totalPrice: number;
  holdExpiresAt: string;
  seats: BookingSeatInfo[];
  createdAt: string;
}

export interface BookingSeatInfo {
  gameSeatId: number;
  price: number;
}
