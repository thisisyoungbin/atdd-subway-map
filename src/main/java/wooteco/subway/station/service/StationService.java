package wooteco.subway.station.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import wooteco.subway.exception.DuplicatedNameException;
import wooteco.subway.station.Station;
import wooteco.subway.station.dto.StationRequest;
import wooteco.subway.station.dto.StationResponse;
import wooteco.subway.station.repository.StationRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class StationService {
    private static final Logger log = LoggerFactory.getLogger(StationService.class);

    private StationRepository stationRepository;

    public StationService(StationRepository stationRepository) {
        this.stationRepository = stationRepository;
    }

    public StationResponse save(StationRequest stationRequest) {
        validateStationName(stationRequest);
        Station station = stationRequest.toEntity();
        Station newStation = stationRepository.save(station);
        log.info("{} 생성 성공.", newStation.getName());
        return new StationResponse(newStation);
    }

    private void validateStationName(StationRequest stationRequest) {
        if (checkNameDuplicate(stationRequest)) {
            throw new DuplicatedNameException("중복된 이름의 역이 존재합니다.");
        }
    }

    private boolean checkNameDuplicate(StationRequest stationRequest) {
        return stationRepository.findByName(stationRequest.getName());
    }

    public List<StationResponse> findAllStations() {
        List<Station> stations = stationRepository.findAll();
        log.info("등록된 지하철 역 조회 성공");
        return stations.stream()
                .map(station -> new StationResponse(station.getId(), station.getName()))
                .collect(Collectors.toList());
    }

    public void delete(Long id) {
        stationRepository.deleteById(id);
        log.info("지하철 역 삭제 성공");
    }
}
