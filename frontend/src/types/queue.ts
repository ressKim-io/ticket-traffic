export type QueueStatus = "WAITING" | "ELIGIBLE" | "COMPLETED" | "ERROR";

export interface QueueEnterRequest {
  gameId: number;
}

export interface QueueStatusResponse {
  gameId: number;
  status: QueueStatus;
  rank: number | null;
  totalWaiting: number | null;
  estimatedWaitSeconds: number | null;
  token: string | null;
}

export interface QueueUpdateMessage {
  gameId: number;
  userId: number | null;
  status: QueueStatus;
  rank: number | null;
  totalWaiting: number | null;
  estimatedWaitSeconds: number | null;
  token: string | null;
}
