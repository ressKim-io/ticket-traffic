package com.sportstix.queue.websocket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class WebSocketSessionRegistryTest {

    private WebSocketSessionRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new WebSocketSessionRegistry();
    }

    @Test
    void registerSubscription_addsUserToGame() {
        registry.registerSubscription(1L, 100L, "session-1");

        assertThat(registry.getSubscribedUsers(1L)).containsExactly(100L);
        assertThat(registry.getConnectionCount(1L)).isEqualTo(1);
        assertThat(registry.getTotalConnections()).isEqualTo(1);
    }

    @Test
    void registerSubscription_multipleUsersPerGame() {
        registry.registerSubscription(1L, 100L, "session-1");
        registry.registerSubscription(1L, 200L, "session-2");
        registry.registerSubscription(1L, 300L, "session-3");

        assertThat(registry.getSubscribedUsers(1L)).containsExactlyInAnyOrder(100L, 200L, 300L);
        assertThat(registry.getConnectionCount(1L)).isEqualTo(3);
        assertThat(registry.getTotalConnections()).isEqualTo(3);
    }

    @Test
    void removeSession_removesUserFromGame() {
        registry.registerSubscription(1L, 100L, "session-1");
        registry.registerSubscription(1L, 200L, "session-2");

        registry.removeSession("session-1");

        assertThat(registry.getSubscribedUsers(1L)).containsExactly(200L);
        assertThat(registry.getTotalConnections()).isEqualTo(1);
    }

    @Test
    void removeSession_lastUser_removesGameEntry() {
        registry.registerSubscription(1L, 100L, "session-1");

        registry.removeSession("session-1");

        assertThat(registry.getSubscribedUsers(1L)).isEmpty();
        assertThat(registry.getConnectionCount(1L)).isEqualTo(0);
    }

    @Test
    void removeSession_unknownSession_noOp() {
        registry.removeSession("unknown-session");

        assertThat(registry.getTotalConnections()).isEqualTo(0);
    }

    @Test
    void getSubscribedUsers_unknownGame_returnsEmpty() {
        Set<Long> users = registry.getSubscribedUsers(999L);

        assertThat(users).isEmpty();
    }

    @Test
    void getSubscribedUsers_returnsImmutableCopy() {
        registry.registerSubscription(1L, 100L, "session-1");

        Set<Long> users = registry.getSubscribedUsers(1L);

        // Returned set should be immutable
        org.junit.jupiter.api.Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> users.add(999L)
        );
    }

    @Test
    void isConnected_registeredUser_returnsTrue() {
        registry.registerSubscription(1L, 100L, "session-1");

        assertThat(registry.isConnected(100L)).isTrue();
        assertThat(registry.isConnected(999L)).isFalse();
    }

    @Test
    void multipleGames_sameSession() {
        registry.registerSubscription(1L, 100L, "session-1");
        registry.registerSubscription(2L, 100L, "session-1");

        assertThat(registry.getSubscribedUsers(1L)).contains(100L);
        assertThat(registry.getSubscribedUsers(2L)).contains(100L);

        registry.removeSession("session-1");

        assertThat(registry.getSubscribedUsers(1L)).isEmpty();
        assertThat(registry.getSubscribedUsers(2L)).isEmpty();
        assertThat(registry.isConnected(100L)).isFalse();
    }

    @Test
    void sameUser_multipleSessions_disconnectOne_retainsOther() {
        // User 100 opens two tabs
        registry.registerSubscription(1L, 100L, "session-1");
        registry.registerSubscription(1L, 100L, "session-2");

        assertThat(registry.getSubscribedUsers(1L)).containsExactly(100L);
        assertThat(registry.getTotalConnections()).isEqualTo(2);

        // Close first tab
        registry.removeSession("session-1");

        // User should still be tracked via second session
        assertThat(registry.getSubscribedUsers(1L)).containsExactly(100L);
        assertThat(registry.isConnected(100L)).isTrue();
        assertThat(registry.getTotalConnections()).isEqualTo(1);

        // Close second tab
        registry.removeSession("session-2");

        assertThat(registry.getSubscribedUsers(1L)).isEmpty();
        assertThat(registry.isConnected(100L)).isFalse();
        assertThat(registry.getTotalConnections()).isEqualTo(0);
    }

    @Test
    void concurrentRegisterAndRemove_noDataLoss() throws InterruptedException {
        int threadCount = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            long userId = i;
            String sessionId = "session-" + i;
            executor.submit(() -> {
                try {
                    registry.registerSubscription(1L, userId, sessionId);
                    Thread.sleep(1);
                    registry.removeSession(sessionId);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        assertThat(registry.getTotalConnections()).isEqualTo(0);
        assertThat(registry.getSubscribedUsers(1L)).isEmpty();
        executor.shutdown();
    }
}
