package com.sportstix.auth.service;

import com.sportstix.auth.domain.Member;
import com.sportstix.auth.domain.Role;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private static final String SECRET = "sportstix-default-secret-key-for-development-only-32bytes!";
    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() {
        provider = new JwtTokenProvider(SECRET, 3600000, 604800000);
    }

    @Test
    void createAccessToken_containsCorrectClaims() {
        Member member = createMember();

        String token = provider.createAccessToken(member);
        Claims claims = provider.parseToken(token);

        assertThat(claims.getSubject()).isEqualTo("1");
        assertThat(claims.get("email", String.class)).isEqualTo("test@test.com");
        assertThat(claims.get("role", String.class)).isEqualTo("USER");
    }

    @Test
    void validateToken_validToken_returnsTrue() {
        Member member = createMember();
        String token = provider.createAccessToken(member);

        assertThat(provider.validateToken(token)).isTrue();
    }

    @Test
    void validateToken_invalidToken_returnsFalse() {
        assertThat(provider.validateToken("invalid-token")).isFalse();
    }

    @Test
    void createRefreshToken_isValid() {
        Member member = createMember();
        String token = provider.createRefreshToken(member);

        assertThat(provider.validateToken(token)).isTrue();
    }

    private Member createMember() {
        // Use reflection to set ID since Builder doesn't expose it
        Member member = Member.builder()
                .email("test@test.com")
                .password("encoded")
                .name("Test User")
                .role(Role.USER)
                .build();
        try {
            var idField = Member.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(member, 1L);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return member;
    }
}
