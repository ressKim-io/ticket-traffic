package com.sportstix.game.service;

import com.sportstix.common.exception.BusinessException;
import com.sportstix.common.response.ErrorCode;
import com.sportstix.game.domain.Seat;
import com.sportstix.game.domain.Section;
import com.sportstix.game.domain.Stadium;
import com.sportstix.game.dto.request.CreateSectionRequest;
import com.sportstix.game.dto.request.CreateStadiumRequest;
import com.sportstix.game.dto.response.StadiumResponse;
import com.sportstix.game.repository.StadiumRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StadiumService {

    private final StadiumRepository stadiumRepository;

    @Transactional
    public StadiumResponse createStadium(CreateStadiumRequest request) {
        Stadium stadium = Stadium.builder()
                .name(request.name())
                .address(request.address())
                .totalCapacity(request.totalCapacity())
                .build();

        for (CreateSectionRequest sectionReq : request.sections()) {
            Section section = Section.builder()
                    .name(sectionReq.name())
                    .grade(sectionReq.grade())
                    .capacity(sectionReq.rows() * sectionReq.seatsPerRow())
                    .stadium(stadium)
                    .build();

            for (int row = 1; row <= sectionReq.rows(); row++) {
                for (int seatNum = 1; seatNum <= sectionReq.seatsPerRow(); seatNum++) {
                    Seat seat = Seat.builder()
                            .rowNumber(String.valueOf(row))
                            .seatNumber(seatNum)
                            .section(section)
                            .build();
                    section.addSeat(seat);
                }
            }
            stadium.addSection(section);
        }

        Stadium saved = stadiumRepository.save(stadium);
        return StadiumResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public StadiumResponse getStadium(Long id) {
        Stadium stadium = stadiumRepository.findByIdWithSections(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Stadium not found: " + id));
        return StadiumResponse.from(stadium);
    }

    @Transactional(readOnly = true)
    public List<StadiumResponse> getAllStadiums() {
        return stadiumRepository.findAll().stream()
                .map(StadiumResponse::summary)
                .toList();
    }
}
