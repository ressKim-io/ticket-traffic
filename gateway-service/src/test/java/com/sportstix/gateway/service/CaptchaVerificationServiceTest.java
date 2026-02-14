package com.sportstix.gateway.service;

import com.sportstix.gateway.config.BotPreventionProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;

class CaptchaVerificationServiceTest {

    private BotPreventionProperties properties;
    private CaptchaVerificationService service;

    @BeforeEach
    void setUp() {
        properties = new BotPreventionProperties();
        // Default: secretKey is empty -> mock mode
        properties.getCaptcha().setSecretKey("");
        properties.getCaptcha().setScoreThreshold(0.5);

        service = new CaptchaVerificationService(properties, WebClient.builder());
    }

    @Test
    void mockMode_withToken_shouldReturnTrue() {
        Boolean result = service.verify("any-token").block();
        assertThat(result).isTrue();
    }

    @Test
    void mockMode_withNullToken_shouldReturnFalse() {
        Boolean result = service.verify(null).block();
        assertThat(result).isFalse();
    }

    @Test
    void mockMode_withBlankToken_shouldReturnFalse() {
        Boolean result = service.verify("  ").block();
        assertThat(result).isFalse();
    }

    @Test
    void isMockMode_whenSecretKeyBlank_shouldBeTrue() {
        assertThat(properties.getCaptcha().isMockMode()).isTrue();
    }

    @Test
    void isMockMode_whenSecretKeyNull_shouldBeTrue() {
        properties.getCaptcha().setSecretKey(null);
        assertThat(properties.getCaptcha().isMockMode()).isTrue();
    }

    @Test
    void isMockMode_whenSecretKeyPresent_shouldBeFalse() {
        properties.getCaptcha().setSecretKey("6LeIxAcTAAAAAGG-vFI1TnRWxMZNFuojJ4WifJWe");
        assertThat(properties.getCaptcha().isMockMode()).isFalse();
    }
}
