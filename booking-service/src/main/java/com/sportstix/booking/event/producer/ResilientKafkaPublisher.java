package com.sportstix.booking.event.producer;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Kafka publish delegate with Resilience4j circuit breaker and retry.
 * Separated from BookingEventProducer to enable Spring AOP proxying.
 * Uses synchronous send (.get()) so that failures are visible to Resilience4j.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResilientKafkaPublisher {

    private static final int SEND_TIMEOUT_SECONDS = 5;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Retry(name = "kafkaProducer")
    @CircuitBreaker(name = "kafkaProducer", fallbackMethod = "publishFallback")
    public void publish(String topic, String key, Object event, String eventName) {
        try {
            kafkaTemplate.send(topic, key, event).get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            throw new RuntimeException("Failed to publish " + eventName + " to " + topic, e.getCause());
        } catch (TimeoutException e) {
            throw new RuntimeException("Timeout publishing " + eventName + " to " + topic, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted publishing " + eventName + " to " + topic, e);
        }
    }

    @SuppressWarnings("unused")
    void publishFallback(String topic, String key, Object event, String eventName, Throwable t) {
        log.error("Circuit breaker open for Kafka producer. Event dropped: {} topic={} key={}",
                eventName, topic, key, t);
        // Reconciliation events bypass outbox intentionally:
        // next reconciliation cycle will re-detect and re-try.
    }
}
