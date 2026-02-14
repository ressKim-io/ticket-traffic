package com.sportstix.auth.controller;

import com.sportstix.auth.dto.request.LoginRequest;
import com.sportstix.auth.dto.request.RefreshRequest;
import com.sportstix.auth.dto.request.SignupRequest;
import com.sportstix.auth.dto.response.MemberResponse;
import com.sportstix.auth.dto.response.TokenResponse;
import com.sportstix.auth.service.AuthService;
import com.sportstix.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "Authentication & user management")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Sign up", description = "Register a new user account")
    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MemberResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ApiResponse.ok(authService.signup(request));
    }

    @Operation(summary = "Login", description = "Authenticate and receive JWT tokens")
    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @Operation(summary = "Refresh token", description = "Exchange refresh token for new access token")
    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiResponse.ok(authService.refresh(request));
    }

    @Operation(summary = "Logout", description = "Invalidate refresh token")
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestHeader("X-User-Id") Long memberId) {
        authService.logout(memberId);
        return ApiResponse.ok();
    }
}
