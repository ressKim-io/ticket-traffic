package com.sportstix.auth.service;

import com.sportstix.auth.domain.Member;
import com.sportstix.auth.dto.request.LoginRequest;
import com.sportstix.auth.dto.request.RefreshRequest;
import com.sportstix.auth.dto.request.SignupRequest;
import com.sportstix.auth.dto.response.MemberResponse;
import com.sportstix.auth.dto.response.TokenResponse;
import com.sportstix.auth.repository.MemberRepository;
import com.sportstix.common.exception.BusinessException;
import com.sportstix.common.response.ErrorCode;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String REFRESH_TOKEN_PREFIX = "auth:refresh:";

    private final MemberRepository memberRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;

    @Transactional
    public MemberResponse signup(SignupRequest request) {
        if (memberRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        Member member = Member.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .name(request.name())
                .build();

        Member saved = memberRepository.save(member);
        return MemberResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        Member member = memberRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGIN_FAILED));

        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        return issueTokens(member);
    }

    @Transactional(readOnly = true)
    public TokenResponse refresh(RefreshRequest request) {
        if (!jwtTokenProvider.validateToken(request.refreshToken())) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        Claims claims = jwtTokenProvider.parseToken(request.refreshToken());

        // Verify token type is refresh (not access)
        String tokenType = claims.get("type", String.class);
        if (!JwtTokenProvider.TOKEN_TYPE_REFRESH.equals(tokenType)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        Long memberId = Long.valueOf(claims.getSubject());

        // Verify refresh token matches the one stored in Redis
        String storedToken = redisTemplate.opsForValue().get(REFRESH_TOKEN_PREFIX + memberId);
        if (storedToken == null || !storedToken.equals(request.refreshToken())) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        return issueTokens(member);
    }

    public void logout(Long memberId) {
        redisTemplate.delete(REFRESH_TOKEN_PREFIX + memberId);
    }

    private TokenResponse issueTokens(Member member) {
        String accessToken = jwtTokenProvider.createAccessToken(member);
        String refreshToken = jwtTokenProvider.createRefreshToken(member);

        redisTemplate.opsForValue().set(
                REFRESH_TOKEN_PREFIX + member.getId(),
                refreshToken,
                jwtTokenProvider.getRefreshTokenExpiry(),
                TimeUnit.MILLISECONDS
        );

        return TokenResponse.of(accessToken, refreshToken);
    }
}
