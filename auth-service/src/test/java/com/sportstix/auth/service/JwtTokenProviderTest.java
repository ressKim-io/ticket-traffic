package com.sportstix.auth.service;

import com.sportstix.auth.domain.Member;
import com.sportstix.auth.domain.Role;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private static String privateKeyPem;
    private static String publicKeyPem;
    private JwtTokenProvider provider;

    @BeforeAll
    static void generateKeyPair() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair keyPair = gen.generateKeyPair();

        privateKeyPem = "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded())
                + "\n-----END PRIVATE KEY-----";
        publicKeyPem = "-----BEGIN PUBLIC KEY-----\n"
                + Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded())
                + "\n-----END PUBLIC KEY-----";
    }

    @BeforeEach
    void setUp() {
        provider = new JwtTokenProvider(privateKeyPem, publicKeyPem, "test-kid", 3600000, 604800000);
    }

    @Test
    void createAccessToken_containsCorrectClaims() {
        Member member = createMember();

        String token = provider.createAccessToken(member);
        Claims claims = provider.parseToken(token);

        assertThat(claims.getSubject()).isEqualTo("1");
        assertThat(claims.get("email", String.class)).isEqualTo("test@test.com");
        assertThat(claims.get("role", String.class)).isEqualTo("USER");
        assertThat(claims.get("type", String.class)).isEqualTo("access");
        assertThat(claims.getIssuer()).isEqualTo("sportstix");
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
        Claims claims = provider.parseToken(token);
        assertThat(claims.get("type", String.class)).isEqualTo("refresh");
    }

    @Test
    void publicKey_isAvailable() {
        assertThat(provider.getPublicKey()).isNotNull();
        assertThat(provider.getKid()).isEqualTo("test-kid");
    }

    private Member createMember() {
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
