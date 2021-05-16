package wooteco.subway.section.service;

import org.springframework.stereotype.Service;
import wooteco.subway.exception.InvalidInsertException;
import wooteco.subway.exception.NotFoundException;
import wooteco.subway.line.Line;
import wooteco.subway.line.dto.LineRequest;
import wooteco.subway.section.Section;
import wooteco.subway.section.Sections;
import wooteco.subway.section.dto.SectionRequest;
import wooteco.subway.section.dto.SectionResponse;
import wooteco.subway.section.repository.SectionDao;
import wooteco.subway.station.Station;
import wooteco.subway.station.dto.StationResponse;
import wooteco.subway.station.service.StationService;

import java.util.List;

@Service
public class SectionService {
    private StationService stationService;
    private SectionDao sectionDao;

    public SectionService(StationService stationService, SectionDao sectionDao) {
        this.stationService = stationService;
        this.sectionDao = sectionDao;
    }

    public void save(Line newLine, LineRequest lineRequest) {
        SectionRequest sectionReq = new SectionRequest(lineRequest);
        validateExistStation(sectionReq.toEntity());

        sectionDao.save(newLine.getId(), sectionReq.toEntity());
    }

    public SectionResponse appendSection(Long lineId, SectionRequest sectionReq) {
        Section newSection = sectionReq.toEntity();
        validateExistStation(newSection);
        Sections sections =  new Sections(sectionDao.findAllByLineId(lineId), newSection);

        if (sections.isOnEdge(newSection)) {
            return saveAtEnd(lineId, newSection);
        }
        return saveAtMiddle(lineId, newSection, sections);
    }

    private void validateExistStation(Section section) {
        if (!stationService.isExistingStation(section.getUpStation())) {
            throw new NotFoundException("등록되지 않은 역은 상행 혹은 하행역으로 추가할 수 없습니다.");
        }
        if (!stationService.isExistingStation(section.getDownStation())) {
            throw new NotFoundException("등록되지 않은 역은 상행 혹은 하행역으로 추가할 수 없습니다.");
        }
    }

    public void validateStations(Long upId, Long downId) {
        stationService.validateStations(upId, downId);
    }

    private SectionResponse saveAtEnd(Long lineId, Section newSection) {
        Section savedSection = sectionDao.save(lineId, newSection);
        return SectionResponse.from(savedSection);
    }

    private SectionResponse saveAtMiddle(Long lineId, Section newSection, Sections sections) {
        if (sections.appendToUp(newSection)) {
            int changedDistance = compareDistanceWhenAppendToUp(lineId, newSection);
            return SectionResponse.from(sectionDao.appendToUp(lineId, newSection, changedDistance));
        }
        if (sections.appendBeforeDown(newSection)) {
            int changedDistance = compareDistanceWhenAppendToBottom(lineId, newSection);
            return SectionResponse.from(sectionDao.appendBeforeDown(lineId, newSection, changedDistance));
        }
        throw new InvalidInsertException("해당 구간에 추가할 수 없습니다.");
    }

    private int compareDistanceWhenAppendToBottom(Long lineId, Section newSection) {
        Section oldSection = sectionDao.findByDownStationId(lineId, newSection.getDownStation());
        return differenceInDistance(newSection, oldSection);
    }

    private int compareDistanceWhenAppendToUp(Long lineId, Section newSection) {
        Section oldSection = sectionDao.findByUpStationId(lineId, newSection.getUpStation());
        return differenceInDistance(newSection, oldSection);
    }

    private int differenceInDistance(Section newSection, Section oldSection) {
        validatesDistance(oldSection, newSection);
        return oldSection.getDistance() - newSection.getDistance();
    }

    private void validatesDistance(Section oldSection, Section newSection) {
        if (newSection.hasLongerDistanceThan(oldSection)) {
            throw new InvalidInsertException("추가하려는 구간의 거리는 기존 구간의 거리를 넘을 수 없습니다.");
        }
    }

    public List<Long> findAllSectionsId(Long lineId) {
        Sections sections = new Sections(sectionDao.findAllByLineId(lineId));
        return sections.toSortedStationIds();
    }

    public void deleteSection(Long lineId, Long stationId) {
        Sections sections = new Sections(sectionDao.findAllByLineId(lineId));
        sections.validateDeletable();
        if (sections.isOnUpEdge(stationId)) {
            sectionDao.deleteFirstSection(lineId, stationId);
            return;
        }

        if (sections.isOnDownEdge(stationId)) {
            sectionDao.deleteLastSection(lineId, stationId);
            return;
        }
        deleteSectionInMiddle(lineId, stationId, sections);
    }

    private void deleteSectionInMiddle(Long lineId, Long stationId, Sections sections) {
        Section before = sections.findSectionByDown(stationId);
        Section after = sections.findSectionByUp(stationId);

        Section newSection = makeNewSection(before, after);

        sectionDao.save(lineId, newSection);
        sectionDao.delete(before);
        sectionDao.delete(after);
    }

    private Section makeNewSection(Section before, Section after) {
        Station newUp = before.getUpStation();
        Station newDown = after.getDownStation();
        int totalDistance = before.plusDistance(after);

        Section newSection = new Section(newUp, newDown, totalDistance);
        return newSection;
    }

    public List<StationResponse> findStationsByIds(List<Long> stationIds) {
        return stationService.findStationsByIds(stationIds);
    }
}
