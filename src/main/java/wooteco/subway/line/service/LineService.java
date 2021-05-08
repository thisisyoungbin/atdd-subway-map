package wooteco.subway.line.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import wooteco.subway.line.Line;
import wooteco.subway.line.dto.LineRequest;
import wooteco.subway.line.dto.LineResponse;
import wooteco.subway.line.repository.LineRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class LineService {

    private static final Logger log = LoggerFactory.getLogger(LineService.class);

    private final LineRepository lineRepository;

    public LineService(LineRepository lineRepository) {
        this.lineRepository = lineRepository;
    }

    public LineResponse save(LineRequest lineRequest) {
        validateLineName(lineRequest);
        Line line = new Line(lineRequest.getName(), lineRequest.getColor());
        Line newLine = lineRepository.save(line);
        log.info(newLine.getName() + " 노선 생성 성공");
        return new LineResponse(newLine);
    }

    private void validateLineName(LineRequest lineRequest) {
        if (checkNameDuplicate(lineRequest)) {
            throw new IllegalArgumentException("중복된 이름의 노선이 존재합니다.");
        }
    }

    private boolean checkNameDuplicate(LineRequest lineRequest) {
        return lineRepository.validateDuplicateName(lineRequest.getName());
    }

    public LineResponse findById(Long id) {
        Line newLine = lineRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 노선입니다."));
        log.info(newLine.getName() + "노선 조회 성공");
        return new LineResponse(newLine);
    }

    public List<LineResponse> findAll() {
        List<Line> lines = lineRepository.findAll();
        log.info("지하철 모든 노선 조회 성공");
        return lines.stream()
                .map(LineResponse::new)
                .collect(Collectors.toList());
    }

    public void update(Long id, LineRequest lineRequest) {
        Line updatedLine = validatesRequest(id, lineRequest);
        lineRepository.updateById(id, updatedLine);
        log.info("노선 정보 수정 완료");
    }

    private Line validatesRequest(Long id, LineRequest lineRequest) {
        Line currentLine = lineRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 노선입니다."));

        validateUsableName(lineRequest.getName(), currentLine.getName());
        return new Line(lineRequest.getName(), lineRequest.getColor());
    }

    private void validateUsableName(String newName, String oldName) {
        if (lineRepository.validateUsableName(newName, oldName)) {
            throw new IllegalArgumentException("변경할 수 없는 이름입니다.");
        }
    }

    public void delete(Long id) {
        lineRepository.delete(id);
        log.info("노선 삭제 성공");
    }
}