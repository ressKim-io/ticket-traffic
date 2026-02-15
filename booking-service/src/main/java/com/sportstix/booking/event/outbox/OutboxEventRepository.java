package com.sportstix.booking.event.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    @Query(value = """
            SELECT * FROM outbox_events
            WHERE status IN ('PENDING', 'RETRYING')
            ORDER BY created_at ASC
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxEvent> findPendingEvents(@Param("batchSize") int batchSize);

    @Modifying
    @Query(value = """
            DELETE FROM outbox_events
            WHERE status = 'PUBLISHED'
            AND published_at < :before
            """, nativeQuery = true)
    int deletePublishedBefore(@Param("before") LocalDateTime before);
}
