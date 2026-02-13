package com.sportstix.game.service;

import com.sportstix.common.exception.BusinessException;
import com.sportstix.game.domain.SeatGrade;
import com.sportstix.game.domain.Stadium;
import com.sportstix.game.dto.request.CreateSectionRequest;
import com.sportstix.game.dto.request.CreateStadiumRequest;
import com.sportstix.game.dto.response.StadiumResponse;
import com.sportstix.game.repository.StadiumRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StadiumServiceTest {

    @InjectMocks
    private StadiumService stadiumService;

    @Mock
    private StadiumRepository stadiumRepository;

    @Test
    void createStadium_createsWithSectionsAndSeats() {
        CreateStadiumRequest request = new CreateStadiumRequest(
                "Test Stadium", "123 Main St", 5000,
                List.of(new CreateSectionRequest("A Block", SeatGrade.VIP, 5, 10))
        );

        given(stadiumRepository.save(any(Stadium.class))).willAnswer(invocation -> invocation.getArgument(0));

        StadiumResponse response = stadiumService.createStadium(request);

        ArgumentCaptor<Stadium> captor = ArgumentCaptor.forClass(Stadium.class);
        verify(stadiumRepository).save(captor.capture());
        Stadium saved = captor.getValue();

        assertThat(saved.getName()).isEqualTo("Test Stadium");
        assertThat(saved.getSections()).hasSize(1);
        assertThat(saved.getSections().get(0).getSeats()).hasSize(50); // 5 rows * 10 seats
        assertThat(saved.getSections().get(0).getCapacity()).isEqualTo(50);
    }

    @Test
    void getStadium_notFound_throwsException() {
        given(stadiumRepository.findByIdWithSections(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> stadiumService.getStadium(999L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void getAllStadiums_returnsSummaries() {
        Stadium s1 = Stadium.builder().name("S1").address("A1").totalCapacity(1000).build();
        Stadium s2 = Stadium.builder().name("S2").address("A2").totalCapacity(2000).build();
        given(stadiumRepository.findAll()).willReturn(List.of(s1, s2));

        List<StadiumResponse> result = stadiumService.getAllStadiums();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).name()).isEqualTo("S1");
        assertThat(result.get(0).sections()).isEmpty();
    }
}
