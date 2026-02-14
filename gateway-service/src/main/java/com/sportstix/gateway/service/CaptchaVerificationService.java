package com.sportstix.gateway.service;

import com.sportstix.gateway.config.BotPreventionProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

@Slf4j
@Service
public class CaptchaVerificationService {

    private static final String RECAPTCHA_VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";
    private static final Duration TIMEOUT = Duration.ofSeconds(3);

    private final BotPreventionProperties properties;
    private final WebClient webClient;

    public CaptchaVerificationService(BotPreventionProperties properties,
                                       WebClient.Builder webClientBuilder) {
        this.properties = properties;
        this.webClient = webClientBuilder.build();
    }

    /**
     * Verify reCAPTCHA v3 token.
     * In mock mode (secretKey blank), returns true if token is present.
     */
    public Mono<Boolean> verify(String token) {
        if (token == null || token.isBlank()) {
            return Mono.just(false);
        }

        if (properties.getCaptcha().isMockMode()) {
            log.debug("CAPTCHA mock mode: accepting token");
            return Mono.just(true);
        }

        return webClient.post()
                .uri(RECAPTCHA_VERIFY_URL)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .bodyValue(String.format("secret=%s&response=%s",
                        properties.getCaptcha().getSecretKey(), token))
                .retrieve()
                .bodyToMono(RecaptchaResponse.class)
                .timeout(TIMEOUT)
                .map(response -> {
                    boolean valid = response.success()
                            && response.score() >= properties.getCaptcha().getScoreThreshold();
                    if (!valid) {
                        log.warn("CAPTCHA verification failed: success={}, score={}",
                                response.success(), response.score());
                    }
                    return valid;
                })
                .onErrorResume(e -> {
                    // Fail-open on timeout/error
                    log.error("CAPTCHA verification error, allowing request: {}", e.getMessage());
                    return Mono.just(true);
                });
    }

    record RecaptchaResponse(boolean success, double score, String action) {}
}
