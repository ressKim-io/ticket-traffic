package com.sportstix.game.dto.response;

import com.sportstix.game.domain.Stadium;

import java.util.List;

public record StadiumResponse(
        Long id,
        String name,
        String address,
        int totalCapacity,
        List<SectionResponse> sections
) {
    public static StadiumResponse from(Stadium stadium) {
        return new StadiumResponse(
                stadium.getId(),
                stadium.getName(),
                stadium.getAddress(),
                stadium.getTotalCapacity(),
                stadium.getSections().stream()
                        .map(SectionResponse::from)
                        .toList()
        );
    }

    public static StadiumResponse summary(Stadium stadium) {
        return new StadiumResponse(
                stadium.getId(),
                stadium.getName(),
                stadium.getAddress(),
                stadium.getTotalCapacity(),
                List.of()
        );
    }
}
