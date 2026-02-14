package com.sportstix.gateway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "gateway.bot-prevention")
public class BotPreventionProperties {

    private Captcha captcha = new Captcha();
    private Fingerprint fingerprint = new Fingerprint();
    private RateLimit rateLimit = new RateLimit();

    @Getter
    @Setter
    public static class Captcha {
        private boolean enabled = true;
        private String secretKey = "";
        private double scoreThreshold = 0.5;
        private List<String> protectedPaths = List.of();

        /** Mock mode when secretKey is blank (frontend not ready) */
        public boolean isMockMode() {
            return secretKey == null || secretKey.isBlank();
        }
    }

    @Getter
    @Setter
    public static class Fingerprint {
        private boolean enabled = true;
        private List<String> blockedUserAgentPatterns = List.of();
    }

    @Getter
    @Setter
    public static class RateLimit {
        private boolean enabled = true;
        private Map<String, EndpointLimit> endpoints = Map.of();
    }

    @Getter
    @Setter
    public static class EndpointLimit {
        private String pathPattern;
        private int ipLimit;
        private int userLimit;
        private int windowSeconds = 60;
    }
}
