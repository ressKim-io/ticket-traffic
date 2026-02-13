package com.sportstix.common.response;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    @Test
    void ok_withData_returnsSuccessResponse() {
        ApiResponse<String> response = ApiResponse.ok("test data");

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isEqualTo("test data");
        assertThat(response.getError()).isNull();
    }

    @Test
    void ok_withoutData_returnsSuccessResponse() {
        ApiResponse<Void> response = ApiResponse.ok();

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isNull();
        assertThat(response.getError()).isNull();
    }

    @Test
    void error_withErrorCode_returnsErrorResponse() {
        ApiResponse<Void> response = ApiResponse.error(ErrorCode.RESOURCE_NOT_FOUND);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getData()).isNull();
        assertThat(response.getError()).isNotNull();
        assertThat(response.getError().getCode()).isEqualTo("C002");
        assertThat(response.getError().getMessage()).isEqualTo("Resource not found");
    }

    @Test
    void error_withCustomMessage_returnsErrorResponse() {
        ApiResponse<Void> response = ApiResponse.error(ErrorCode.INVALID_INPUT, "Name is required");

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getError().getCode()).isEqualTo("C001");
        assertThat(response.getError().getMessage()).isEqualTo("Name is required");
    }
}
