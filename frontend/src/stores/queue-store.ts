import { create } from "zustand";
import type { QueueStatus } from "@/types";

interface QueueState {
  gameId: number | null;
  status: QueueStatus | null;
  rank: number | null;
  totalWaiting: number | null;
  estimatedWaitSeconds: number | null;
  token: string | null;
  connected: boolean;

  setGameId: (gameId: number) => void;
  updatePosition: (data: {
    status: QueueStatus;
    rank: number | null;
    totalWaiting: number | null;
    estimatedWaitSeconds: number | null;
    token: string | null;
  }) => void;
  setConnected: (connected: boolean) => void;
  reset: () => void;
}

const initialState = {
  gameId: null,
  status: null,
  rank: null,
  totalWaiting: null,
  estimatedWaitSeconds: null,
  token: null,
  connected: false,
};

export const useQueueStore = create<QueueState>()((set) => ({
  ...initialState,

  setGameId: (gameId) => set({ gameId }),

  updatePosition: (data) =>
    set({
      status: data.status,
      rank: data.rank,
      totalWaiting: data.totalWaiting,
      estimatedWaitSeconds: data.estimatedWaitSeconds,
      token: data.token,
    }),

  setConnected: (connected) => set({ connected }),

  reset: () => set(initialState),
}));
