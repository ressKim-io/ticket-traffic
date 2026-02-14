"use client";

import { useEffect, useCallback, useRef } from "react";
import { useMutation } from "@tanstack/react-query";
import type { Client } from "@stomp/stompjs";
import { apiClient } from "@/lib";
import { getStompClient, subscribe, disconnectStomp } from "@/lib/ws-client";
import { useQueueStore } from "@/stores";
import { useAuthStore } from "@/stores";
import type {
  ApiResponse,
  QueueStatusResponse,
  QueueUpdateMessage,
} from "@/types";

/**
 * Enter the queue for a game.
 */
export function useQueueEnter() {
  const { updatePosition, setGameId } = useQueueStore();

  return useMutation({
    mutationFn: async (gameId: number) => {
      const { data } = await apiClient.post<ApiResponse<QueueStatusResponse>>(
        "/queue/enter",
        { gameId }
      );
      return data;
    },
    onSuccess: (res, gameId) => {
      if (res.success && res.data) {
        setGameId(gameId);
        updatePosition({
          status: res.data.status,
          rank: res.data.rank,
          totalWaiting: res.data.totalWaiting,
          estimatedWaitSeconds: res.data.estimatedWaitSeconds,
          token: res.data.token,
        });
      }
    },
  });
}

/**
 * Check current queue status.
 */
export function useQueueStatus() {
  const { updatePosition } = useQueueStore();

  return useMutation({
    mutationFn: async (gameId: number) => {
      const { data } = await apiClient.get<ApiResponse<QueueStatusResponse>>(
        "/queue/status",
        { params: { gameId } }
      );
      return data;
    },
    onSuccess: (res) => {
      if (res.success && res.data) {
        updatePosition({
          status: res.data.status,
          rank: res.data.rank,
          totalWaiting: res.data.totalWaiting,
          estimatedWaitSeconds: res.data.estimatedWaitSeconds,
          token: res.data.token,
        });
      }
    },
  });
}

/**
 * Leave the queue.
 */
export function useQueueLeave() {
  const { reset } = useQueueStore();

  return useMutation({
    mutationFn: async (gameId: number) => {
      const { data } = await apiClient.delete<ApiResponse<void>>(
        "/queue/leave",
        { params: { gameId } }
      );
      return data;
    },
    onSuccess: () => {
      disconnectStomp();
      reset();
    },
  });
}

/**
 * Subscribe to real-time queue updates via WebSocket.
 * Handles Strict Mode double-mount and proper cleanup.
 */
export function useQueueWebSocket(gameId: number) {
  const { updatePosition, setConnected } = useQueueStore();
  const user = useAuthStore((s) => s.user);
  const subscriptionsRef = useRef<Array<{ unsubscribe: () => void }>>([]);
  const clientRef = useRef<Client | null>(null);

  const handleMessage = useCallback(
    (msg: QueueUpdateMessage) => {
      updatePosition({
        status: msg.status,
        rank: msg.rank,
        totalWaiting: msg.totalWaiting,
        estimatedWaitSeconds: msg.estimatedWaitSeconds,
        token: msg.token,
      });
    },
    [updatePosition]
  );

  useEffect(() => {
    if (!user?.id || !gameId) return;

    // Prevent duplicate connections (React Strict Mode)
    if (clientRef.current?.active) return;

    const client = getStompClient(user.id);
    clientRef.current = client;

    client.onConnect = () => {
      setConnected(true);

      // Personal topic (includes token)
      const personalSub = subscribe<QueueUpdateMessage>(
        client,
        `/topic/queue/${gameId}/${user.id}`,
        handleMessage
      );

      // Public topic (general stats - uses getState for fresh data)
      const publicSub = subscribe<QueueUpdateMessage>(
        client,
        `/topic/queue/${gameId}`,
        (msg) => {
          const current = useQueueStore.getState();
          if (current.status === "WAITING" && msg.totalWaiting != null) {
            useQueueStore.setState({ totalWaiting: msg.totalWaiting });
          }
        }
      );

      subscriptionsRef.current = [personalSub, publicSub];
    };

    client.onDisconnect = () => {
      setConnected(false);
    };

    client.onStompError = (frame) => {
      console.error("[Queue WS] STOMP error:", frame.headers.message);
      setConnected(false);
    };

    if (!client.active) {
      client.activate();
    }

    return () => {
      subscriptionsRef.current.forEach((s) => s.unsubscribe());
      subscriptionsRef.current = [];
      if (clientRef.current) {
        clientRef.current.deactivate();
        clientRef.current = null;
      }
      setConnected(false);
    };
  }, [gameId, user?.id, handleMessage, setConnected]);
}
