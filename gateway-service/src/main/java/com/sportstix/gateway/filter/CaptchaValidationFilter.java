package com.sportstix.gateway.filter;

import com.sportstix.gateway.config.BotPreventionProperties;
import com.sportstix.gateway.service.CaptchaVerificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class CaptchaValidationFilter implements GlobalFilter, Ordered {

    private static final String CAPTCHA_TOKEN_HEADER = "X-Captcha-Token";
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final BotPreventionProperties properties;
    private final CaptchaVerificationService captchaService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!properties.getCaptcha().isEnabled()) {
            return chain.filter(exchange);
        }

        String path = exchange.getRequest().getURI().getPath();
        if (!isProtectedPath(path)) {
            return chain.filter(exchange);
        }

        String token = exchange.getRequest().getHeaders().getFirst(CAPTCHA_TOKEN_HEADER);
        if (token == null || token.isBlank()) {
            log.warn("Missing CAPTCHA token for protected path: {}", path);
            return RequestUtils.writeJsonResponse(exchange, HttpStatus.FORBIDDEN,
                    "CAPTCHA token required");
        }

        return captchaService.verify(token)
                .flatMap(valid -> {
                    if (!valid) {
                        log.warn("CAPTCHA verification failed for path: {}", path);
                        return RequestUtils.writeJsonResponse(exchange, HttpStatus.FORBIDDEN,
                                "CAPTCHA verification failed");
                    }
                    return chain.filter(exchange);
                });
    }

    @Override
    public int getOrder() {
        return -70;
    }

    private boolean isProtectedPath(String path) {
        return properties.getCaptcha().getProtectedPaths().stream()
                .anyMatch(pattern -> PATH_MATCHER.match(pattern, path));
    }
}
