package com.sportstix.common.config;

import com.sportstix.common.event.Topics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

/**
 * Auto-creates Kafka topics with appropriate partition counts.
 * Partition counts are sized to match KEDA max replicas per service.
 * Only activates when spring.kafka.bootstrap-servers is configured.
 */
@AutoConfiguration
@ConditionalOnClass(KafkaAdmin.class)
@ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
public class KafkaTopicConfig {

    private static final short REPLICATION_FACTOR = 1;

    // -- Booking topics: 8 partitions (high throughput, seat-level concurrency) --

    @Bean
    public NewTopic bookingCreatedTopic() {
        return buildTopic(Topics.BOOKING_CREATED, Topics.PARTITIONS_BOOKING);
    }

    @Bean
    public NewTopic bookingCreatedDlt() {
        return buildDlt(Topics.BOOKING_CREATED, Topics.PARTITIONS_BOOKING);
    }

    @Bean
    public NewTopic bookingConfirmedTopic() {
        return buildTopic(Topics.BOOKING_CONFIRMED, Topics.PARTITIONS_BOOKING);
    }

    @Bean
    public NewTopic bookingConfirmedDlt() {
        return buildDlt(Topics.BOOKING_CONFIRMED, Topics.PARTITIONS_BOOKING);
    }

    @Bean
    public NewTopic bookingCancelledTopic() {
        return buildTopic(Topics.BOOKING_CANCELLED, Topics.PARTITIONS_BOOKING);
    }

    @Bean
    public NewTopic bookingCancelledDlt() {
        return buildDlt(Topics.BOOKING_CANCELLED, Topics.PARTITIONS_BOOKING);
    }

    // -- Seat topics: 8 partitions (same as booking, tightly coupled) --

    @Bean
    public NewTopic seatHeldTopic() {
        return buildTopic(Topics.SEAT_HELD, Topics.PARTITIONS_SEAT);
    }

    @Bean
    public NewTopic seatHeldDlt() {
        return buildDlt(Topics.SEAT_HELD, Topics.PARTITIONS_SEAT);
    }

    @Bean
    public NewTopic seatReleasedTopic() {
        return buildTopic(Topics.SEAT_RELEASED, Topics.PARTITIONS_SEAT);
    }

    @Bean
    public NewTopic seatReleasedDlt() {
        return buildDlt(Topics.SEAT_RELEASED, Topics.PARTITIONS_SEAT);
    }

    // -- Payment topics: 6 partitions (moderate throughput) --

    @Bean
    public NewTopic paymentCompletedTopic() {
        return buildTopic(Topics.PAYMENT_COMPLETED, Topics.PARTITIONS_PAYMENT);
    }

    @Bean
    public NewTopic paymentCompletedDlt() {
        return buildDlt(Topics.PAYMENT_COMPLETED, Topics.PARTITIONS_PAYMENT);
    }

    @Bean
    public NewTopic paymentFailedTopic() {
        return buildTopic(Topics.PAYMENT_FAILED, Topics.PARTITIONS_PAYMENT);
    }

    @Bean
    public NewTopic paymentFailedDlt() {
        return buildDlt(Topics.PAYMENT_FAILED, Topics.PARTITIONS_PAYMENT);
    }

    @Bean
    public NewTopic paymentRefundedTopic() {
        return buildTopic(Topics.PAYMENT_REFUNDED, Topics.PARTITIONS_PAYMENT);
    }

    @Bean
    public NewTopic paymentRefundedDlt() {
        return buildDlt(Topics.PAYMENT_REFUNDED, Topics.PARTITIONS_PAYMENT);
    }

    // -- Queue topics: 4 partitions (moderate, Redis-backed primary) --

    @Bean
    public NewTopic queueEnteredTopic() {
        return buildTopic(Topics.QUEUE_ENTERED, Topics.PARTITIONS_QUEUE);
    }

    @Bean
    public NewTopic queueEnteredDlt() {
        return buildDlt(Topics.QUEUE_ENTERED, Topics.PARTITIONS_QUEUE);
    }

    @Bean
    public NewTopic queueTokenIssuedTopic() {
        return buildTopic(Topics.QUEUE_TOKEN_ISSUED, Topics.PARTITIONS_QUEUE);
    }

    @Bean
    public NewTopic queueTokenIssuedDlt() {
        return buildDlt(Topics.QUEUE_TOKEN_ISSUED, Topics.PARTITIONS_QUEUE);
    }

    // -- Game topics: 3 partitions (low frequency, bulk events) --

    @Bean
    public NewTopic gameSeatInitializedTopic() {
        return buildTopic(Topics.GAME_SEAT_INITIALIZED, Topics.PARTITIONS_GAME);
    }

    @Bean
    public NewTopic gameSeatInitializedDlt() {
        return buildDlt(Topics.GAME_SEAT_INITIALIZED, Topics.PARTITIONS_GAME);
    }

    @Bean
    public NewTopic gameInfoUpdatedTopic() {
        return buildTopic(Topics.GAME_INFO_UPDATED, Topics.PARTITIONS_GAME);
    }

    @Bean
    public NewTopic gameInfoUpdatedDlt() {
        return buildDlt(Topics.GAME_INFO_UPDATED, Topics.PARTITIONS_GAME);
    }

    private NewTopic buildTopic(String name, int partitions) {
        return TopicBuilder.name(name)
                .partitions(partitions)
                .replicas(REPLICATION_FACTOR)
                .build();
    }

    private NewTopic buildDlt(String name, int partitions) {
        return TopicBuilder.name(Topics.dlt(name))
                .partitions(partitions)
                .replicas(REPLICATION_FACTOR)
                .build();
    }
}
