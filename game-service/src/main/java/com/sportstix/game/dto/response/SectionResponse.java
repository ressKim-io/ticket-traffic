package com.sportstix.game.dto.response;

import com.sportstix.game.domain.Section;

import java.math.BigDecimal;

public record SectionResponse(
        Long id,
        String name,
        String grade,
        int capacity,
        BigDecimal defaultPrice
) {
    public static SectionResponse from(Section section) {
        return new SectionResponse(
                section.getId(),
                section.getName(),
                section.getGrade().name(),
                section.getCapacity(),
                section.getGrade().getDefaultPrice()
        );
    }
}
