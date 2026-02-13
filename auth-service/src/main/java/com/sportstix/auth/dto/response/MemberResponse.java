package com.sportstix.auth.dto.response;

import com.sportstix.auth.domain.Member;

public record MemberResponse(
        Long id,
        String email,
        String name,
        String role
) {
    public static MemberResponse from(Member member) {
        return new MemberResponse(
                member.getId(),
                member.getEmail(),
                member.getName(),
                member.getRole().name()
        );
    }
}
