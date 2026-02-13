package com.sportstix.game.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@Getter
@RequiredArgsConstructor
public enum SeatGrade {

    VIP("VIP", BigDecimal.valueOf(150000)),
    R("R석", BigDecimal.valueOf(100000)),
    S("S석", BigDecimal.valueOf(70000)),
    A("A석", BigDecimal.valueOf(50000)),
    B("B석", BigDecimal.valueOf(30000));

    private final String displayName;
    private final BigDecimal defaultPrice;
}
