package com.sportstix.auth.service;

import com.sportstix.auth.domain.Member;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtTokenProvider {

    public static final String TOKEN_TYPE_ACCESS = "access";
    public static final String TOKEN_TYPE_REFRESH = "refresh";
    public static final String ISSUER = "sportstix";

    private final PrivateKey privateKey;
    @Getter
    private final RSAPublicKey publicKey;
    private final JwtParser jwtParser;
    private final long accessTokenExpiry;
    @Getter
    private final long refreshTokenExpiry;
    @Getter
    private final String kid;

    public JwtTokenProvider(
            @Value("${jwt.private-key}") String privateKeyPem,
            @Value("${jwt.public-key}") String publicKeyPem,
            @Value("${jwt.kid:sportstix-1}") String kid,
            @Value("${jwt.access-token-expiry:3600000}") long accessTokenExpiry,
            @Value("${jwt.refresh-token-expiry:604800000}") long refreshTokenExpiry) {
        this.privateKey = parsePrivateKey(privateKeyPem);
        this.publicKey = (RSAPublicKey) parsePublicKey(publicKeyPem);
        this.kid = kid;
        this.jwtParser = Jwts.parser().verifyWith(this.publicKey).build();
        this.accessTokenExpiry = accessTokenExpiry;
        this.refreshTokenExpiry = refreshTokenExpiry;
    }

    public String createAccessToken(Member member) {
        return createToken(member, accessTokenExpiry, TOKEN_TYPE_ACCESS);
    }

    public String createRefreshToken(Member member) {
        return createToken(member, refreshTokenExpiry, TOKEN_TYPE_REFRESH);
    }

    public Claims parseToken(String token) {
        try {
            return jwtParser.parseSignedClaims(token).getPayload();
        } catch (JwtException e) {
            throw new JwtException("Invalid token: " + e.getMessage(), e);
        }
    }

    public boolean validateToken(String token) {
        try {
            jwtParser.parseSignedClaims(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    private String createToken(Member member, long expiry, String tokenType) {
        Date now = new Date();
        return Jwts.builder()
                .header().keyId(kid).and()
                .issuer(ISSUER)
                .subject(String.valueOf(member.getId()))
                .claim("email", member.getEmail())
                .claim("role", member.getRole().name())
                .claim("type", tokenType)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiry))
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    private static PrivateKey parsePrivateKey(String pem) {
        try {
            String base64 = pem
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s+", "");
            byte[] decoded = Base64.getDecoder().decode(base64);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
            return KeyFactory.getInstance("RSA").generatePrivate(spec);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse RSA private key", e);
        }
    }

    private static PublicKey parsePublicKey(String pem) {
        try {
            String base64 = pem
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s+", "");
            byte[] decoded = Base64.getDecoder().decode(base64);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
            return KeyFactory.getInstance("RSA").generatePublic(spec);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse RSA public key", e);
        }
    }
}
