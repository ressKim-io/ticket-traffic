package com.sportstix.queue.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "queue")
public class QueueProperties {

    private int batchSize = 100;
    private int tokenTtlSeconds = 600;
    private long processIntervalMs = 3000;
}
