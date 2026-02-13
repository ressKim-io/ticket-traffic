package com.sportstix.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimiterConfig {

    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            // Respect X-Forwarded-For when behind proxy/load balancer
            String xff = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) {
                return Mono.just(xff.split(",")[0].trim());
            }
            var remoteAddr = exchange.getRequest().getRemoteAddress();
            return Mono.just(remoteAddr != null
                    ? remoteAddr.getAddress().getHostAddress()
                    : "unknown");
        };
    }
}
