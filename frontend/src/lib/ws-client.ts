import { Client, type IMessage, type StompSubscription } from "@stomp/stompjs";
import SockJS from "sockjs-client";

const WS_BASE_URL =
  process.env.NEXT_PUBLIC_WS_URL || "http://localhost:8080";

let stompClient: Client | null = null;

export interface WsSubscription {
  unsubscribe: () => void;
}

/**
 * Get or create a shared STOMP client over SockJS.
 * If the userId has changed, deactivates the old client first.
 */
export function getStompClient(userId: number): Client {
  // Deactivate existing client if userId changed
  if (
    stompClient &&
    stompClient.connectHeaders?.["X-User-Id"] !== String(userId)
  ) {
    stompClient.deactivate();
    stompClient = null;
  }

  if (stompClient?.active) return stompClient;

  const client = new Client({
    webSocketFactory: () => new SockJS(`${WS_BASE_URL}/ws/queue`),
    connectHeaders: {
      "X-User-Id": String(userId),
    },
    reconnectDelay: 3000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    debug: (msg) => {
      if (process.env.NODE_ENV === "development") {
        console.debug("[STOMP]", msg);
      }
    },
  });

  stompClient = client;
  return client;
}

/**
 * Subscribe to a STOMP destination after ensuring the client is connected.
 * Chains onConnect handlers to safely support multiple subscriptions.
 */
export function subscribe<T>(
  client: Client,
  destination: string,
  callback: (body: T) => void
): WsSubscription {
  let sub: StompSubscription | null = null;

  const doSubscribe = () => {
    sub = client.subscribe(destination, (message: IMessage) => {
      try {
        const parsed = JSON.parse(message.body) as T;
        callback(parsed);
      } catch {
        console.warn("[STOMP] Failed to parse message:", message.body);
      }
    });
  };

  if (client.connected) {
    doSubscribe();
  } else {
    // Chain onConnect handlers instead of overwriting
    const previousOnConnect = client.onConnect;
    client.onConnect = (frame) => {
      previousOnConnect?.(frame);
      doSubscribe();
    };
  }

  return {
    unsubscribe: () => {
      sub?.unsubscribe();
    },
  };
}

/**
 * Deactivate the shared STOMP client and clean up.
 */
export function disconnectStomp(): void {
  if (stompClient) {
    stompClient.deactivate();
    stompClient = null;
  }
}
