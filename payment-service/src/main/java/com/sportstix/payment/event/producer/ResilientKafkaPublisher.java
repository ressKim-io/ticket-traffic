package com.sportstix.payment.event.producer;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Kafka publish delegate with Resilience4j circuit breaker and retry.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResilientKafkaPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @CircuitBreaker(name = "kafkaProducer", fallbackMethod = "publishFallback")
    @Retry(name = "kafkaProducer")
    public void publish(String topic, String key, Object event, String eventName) {
        kafkaTemplate.send(topic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish {}: topic={}", eventName, topic, ex);
                    } else {
                        log.info("Published {}: topic={}, key={}", eventName, topic, key);
                    }
                });
    }

    @SuppressWarnings("unused")
    private void publishFallback(String topic, String key, Object event, String eventName, Throwable t) {
        log.error("Circuit breaker open for Kafka producer. Event lost: {} topic={} key={}",
                eventName, topic, key, t);
    }
}
