package com.sportstix.booking.event.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    @Query("""
            SELECT o FROM OutboxEvent o
            WHERE o.status IN ('PENDING', 'RETRYING')
            ORDER BY o.createdAt ASC
            LIMIT :batchSize
            """)
    List<OutboxEvent> findPendingEvents(int batchSize);

    @Modifying
    @Query("""
            DELETE FROM OutboxEvent o
            WHERE o.status = 'PUBLISHED'
            AND o.publishedAt < :before
            """)
    int deletePublishedBefore(LocalDateTime before);
}
