package com.sportstix.auth.controller;

import com.sportstix.auth.service.JwtTokenProvider;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@RestController
public class JwksController {

    private final Map<String, Object> jwksResponse;

    public JwksController(JwtTokenProvider jwtTokenProvider) {
        RSAPublicKey publicKey = jwtTokenProvider.getPublicKey();
        String kid = jwtTokenProvider.getKid();

        Map<String, Object> jwk = Map.of(
                "kty", "RSA",
                "alg", "RS256",
                "use", "sig",
                "kid", kid,
                "n", base64UrlEncode(publicKey.getModulus().toByteArray()),
                "e", base64UrlEncode(publicKey.getPublicExponent().toByteArray())
        );
        this.jwksResponse = Map.of("keys", List.of(jwk));
    }

    @GetMapping(value = "/.well-known/jwks.json", produces = "application/json")
    public ResponseEntity<Map<String, Object>> jwks() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePublic())
                .body(jwksResponse);
    }

    private static String base64UrlEncode(byte[] bytes) {
        // Strip leading zero byte if present (BigInteger sign bit)
        if (bytes.length > 0 && bytes[0] == 0) {
            byte[] stripped = new byte[bytes.length - 1];
            System.arraycopy(bytes, 1, stripped, 0, stripped.length);
            bytes = stripped;
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
