package com.sportstix.auth.service;

import com.sportstix.auth.domain.Member;
import com.sportstix.auth.domain.Role;
import com.sportstix.auth.dto.request.LoginRequest;
import com.sportstix.auth.dto.request.SignupRequest;
import com.sportstix.auth.dto.response.MemberResponse;
import com.sportstix.auth.dto.response.TokenResponse;
import com.sportstix.auth.repository.MemberRepository;
import com.sportstix.common.exception.BusinessException;
import com.sportstix.common.response.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    @Test
    void signup_success() {
        SignupRequest request = new SignupRequest("test@test.com", "password123", "Test User");
        given(memberRepository.existsByEmail("test@test.com")).willReturn(false);
        given(passwordEncoder.encode("password123")).willReturn("encoded");
        given(memberRepository.save(any(Member.class))).willAnswer(invocation -> {
            Member m = invocation.getArgument(0);
            return Member.builder()
                    .email(m.getEmail())
                    .password(m.getPassword())
                    .name(m.getName())
                    .build();
        });

        MemberResponse response = authService.signup(request);

        assertThat(response.email()).isEqualTo("test@test.com");
        assertThat(response.name()).isEqualTo("Test User");
    }

    @Test
    void signup_duplicateEmail_throwsException() {
        SignupRequest request = new SignupRequest("dup@test.com", "password123", "Dup");
        given(memberRepository.existsByEmail("dup@test.com")).willReturn(true);

        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_EMAIL);
    }

    @Test
    void login_success() {
        LoginRequest request = new LoginRequest("test@test.com", "password123");
        Member member = Member.builder()
                .email("test@test.com")
                .password("encoded")
                .name("Test")
                .build();

        given(memberRepository.findByEmail("test@test.com")).willReturn(Optional.of(member));
        given(passwordEncoder.matches("password123", "encoded")).willReturn(true);
        given(jwtTokenProvider.createAccessToken(member)).willReturn("access-token");
        given(jwtTokenProvider.createRefreshToken(member)).willReturn("refresh-token");
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        TokenResponse response = authService.login(request);

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
    }

    @Test
    void login_wrongPassword_throwsException() {
        LoginRequest request = new LoginRequest("test@test.com", "wrong");
        Member member = Member.builder()
                .email("test@test.com")
                .password("encoded")
                .name("Test")
                .build();

        given(memberRepository.findByEmail("test@test.com")).willReturn(Optional.of(member));
        given(passwordEncoder.matches("wrong", "encoded")).willReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.LOGIN_FAILED);
    }

    @Test
    void login_nonExistentEmail_throwsException() {
        LoginRequest request = new LoginRequest("none@test.com", "password123");
        given(memberRepository.findByEmail("none@test.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.LOGIN_FAILED);
    }
}
