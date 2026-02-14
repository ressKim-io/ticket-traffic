package com.sportstix.auth.controller;

import com.sportstix.auth.service.JwtTokenProvider;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JwksControllerTest {

    private static String privateKeyPem;
    private static String publicKeyPem;
    private static KeyPair keyPair;

    @BeforeAll
    static void generateKeyPair() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        keyPair = gen.generateKeyPair();

        privateKeyPem = "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded())
                + "\n-----END PRIVATE KEY-----";
        publicKeyPem = "-----BEGIN PUBLIC KEY-----\n"
                + Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded())
                + "\n-----END PUBLIC KEY-----";
    }

    @Test
    @SuppressWarnings("unchecked")
    void jwks_returnsValidKeySet() {
        JwtTokenProvider provider = new JwtTokenProvider(privateKeyPem, publicKeyPem, "test-kid", 3600000, 604800000);
        JwksController controller = new JwksController(provider);

        Map<String, Object> response = controller.jwks().getBody();

        assertThat(response).containsKey("keys");
        List<Map<String, Object>> keys = (List<Map<String, Object>>) response.get("keys");
        assertThat(keys).hasSize(1);

        Map<String, Object> jwk = keys.get(0);
        assertThat(jwk.get("kty")).isEqualTo("RSA");
        assertThat(jwk.get("alg")).isEqualTo("RS256");
        assertThat(jwk.get("use")).isEqualTo("sig");
        assertThat(jwk.get("kid")).isEqualTo("test-kid");
        assertThat(jwk.get("n")).isNotNull();
        assertThat(jwk.get("e")).isNotNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void jwks_publicKeyCanBeReconstructed() throws Exception {
        JwtTokenProvider provider = new JwtTokenProvider(privateKeyPem, publicKeyPem, "test-kid", 3600000, 604800000);
        JwksController controller = new JwksController(provider);

        Map<String, Object> response = controller.jwks().getBody();
        List<Map<String, Object>> keys = (List<Map<String, Object>>) response.get("keys");
        Map<String, Object> jwk = keys.get(0);

        // Reconstruct RSA public key from JWKS n and e values
        byte[] nBytes = Base64.getUrlDecoder().decode((String) jwk.get("n"));
        byte[] eBytes = Base64.getUrlDecoder().decode((String) jwk.get("e"));
        BigInteger modulus = new BigInteger(1, nBytes);
        BigInteger exponent = new BigInteger(1, eBytes);

        RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);
        RSAPublicKey reconstructed = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);

        assertThat(reconstructed.getModulus()).isEqualTo(provider.getPublicKey().getModulus());
        assertThat(reconstructed.getPublicExponent()).isEqualTo(provider.getPublicKey().getPublicExponent());
    }

    @Test
    void jwks_tokenSignedByPrivateKeyValidatesWithReconstructedKey() throws Exception {
        JwtTokenProvider provider = new JwtTokenProvider(privateKeyPem, publicKeyPem, "test-kid", 3600000, 604800000);
        JwksController controller = new JwksController(provider);

        // Sign a token with private key
        String token = Jwts.builder()
                .subject("test-user")
                .signWith(keyPair.getPrivate(), Jwts.SIG.RS256)
                .compact();

        // Reconstruct public key from JWKS
        @SuppressWarnings("unchecked")
        Map<String, Object> response = controller.jwks().getBody();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> keys = (List<Map<String, Object>>) response.get("keys");
        Map<String, Object> jwk = keys.get(0);

        byte[] nBytes = Base64.getUrlDecoder().decode((String) jwk.get("n"));
        byte[] eBytes = Base64.getUrlDecoder().decode((String) jwk.get("e"));
        RSAPublicKeySpec spec = new RSAPublicKeySpec(new BigInteger(1, nBytes), new BigInteger(1, eBytes));
        RSAPublicKey reconstructed = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);

        // Verify token with reconstructed public key
        String subject = Jwts.parser()
                .verifyWith(reconstructed)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();

        assertThat(subject).isEqualTo("test-user");
    }
}
