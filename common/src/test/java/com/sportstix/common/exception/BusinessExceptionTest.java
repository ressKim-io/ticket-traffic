package com.sportstix.common.exception;

import com.sportstix.common.response.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessExceptionTest {

    @Test
    void constructor_withErrorCode_setsDefaultMessage() {
        BusinessException ex = new BusinessException(ErrorCode.SEAT_ALREADY_HELD);

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.SEAT_ALREADY_HELD);
        assertThat(ex.getMessage()).isEqualTo("Seat is already held");
    }

    @Test
    void constructor_withCustomMessage_overridesDefault() {
        BusinessException ex = new BusinessException(ErrorCode.SEAT_ALREADY_HELD, "Seat A-12 is taken");

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.SEAT_ALREADY_HELD);
        assertThat(ex.getMessage()).isEqualTo("Seat A-12 is taken");
    }
}
