package wooteco.subway.section.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import wooteco.subway.line.Line;
import wooteco.subway.section.Section;
import wooteco.subway.station.Station;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Objects;

@Repository
public class JdbcSectionDao implements SectionDao {
    private JdbcTemplate jdbcTemplate;
    private final RowMapper<Section> sectionMapper = (rs, rowNum) -> new Section (
            rs.getLong("id"),
            new Line(rs.getLong("line_id"), null, null, null),
            new Station(rs.getLong("up_station_id"), null),
            new Station(rs.getLong("down_station_id"), null),
            rs.getInt("distance")
    );

    public JdbcSectionDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Section save(Long lineId, Section section) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        String query = "INSERT INTO section (line_id, up_station_id, down_station_id, distance) VALUES (?, ?, ?, ?)";
        jdbcTemplate.update(con -> {
            PreparedStatement pstmt = con.prepareStatement(query, new String[]{"id"});
            pstmt.setLong(1, lineId);
            pstmt.setLong(2, section.getUpStation().getId());
            pstmt.setLong(3, section.getDownStation().getId());
            pstmt.setInt(4, section.getDistance());
            return pstmt;
        }, keyHolder);

        Long id = Objects.requireNonNull(keyHolder.getKey()).longValue();
        return new Section(id, new Line(lineId, null, null), section);
    }

    @Override
    public List<Section> findAllByLineId(Long lineId) {
        String query = "SELECT * FROM section WHERE line_id = ?";
        return jdbcTemplate.query(query, sectionMapper, lineId);
    }

    @Override
    public Section findByUpStationId(Long lineId, Station upStation) {
        String query = "SELECT * FROM section WHERE up_station_id = ? AND line_id = ?";
        return jdbcTemplate.query(query, sectionMapper, upStation.getId(), lineId).get(0);
    }

    @Override
    public Section findByDownStationId(Long lineId, Station downStation) {
        String query = "SELECT * FROM section WHERE down_station_id = ? AND line_id = ?";
        return jdbcTemplate.query(query, sectionMapper, downStation.getId(), lineId).get(0);
    }

    @Override
    public void delete(Section section) {
        String query = "DELETE FROM section WHERE id = ?";
        jdbcTemplate.update(query, section.getId());
    }

    @Override
    public Section appendToUp(Long lineId, Section newSection, int changedDistance) {
        updateAndAppendToUp(lineId, newSection, changedDistance);
        return save(lineId, newSection);
    }

    @Override
    public Section appendBeforeDown(Long lineId, Section newSection, int changedDistance) {
        updateAndAppendBeforeDown(newSection, changedDistance);
        return save(lineId, newSection);
    }

    @Override
    public void updateAndAppendToUp(Long lineId, Section newSection, int changedDistance) {
        String query = "UPDATE section SET up_station_id = ?, distance = ? WHERE up_station_id = ? AND line_id = ?";
        jdbcTemplate.update(query, newSection.getDownStationId(), changedDistance, newSection.getUpStationId(), lineId);
    }

    @Override
    public void updateAndAppendBeforeDown(Section newSection, int changedDistance) {
        String query = "UPDATE section SET down_station_id = ?, distance = ? WHERE down_station_id = ?";
        jdbcTemplate.update(query, newSection.getUpStationId(), changedDistance, newSection.getDownStationId());
    }

    @Override
    public void deleteFirstSection(Long lineId, Long stationId) {
        String query = "DELETE FROM section WHERE line_id = ? AND up_station_id = ?";
        jdbcTemplate.update(query, lineId, stationId);
    }

    @Override
    public void deleteLastSection(Long lineId, Long stationId) {
        String query = "DELETE FROM section WHERE line_id = ? AND down_station_id = ?";
        jdbcTemplate.update(query, lineId, stationId);
    }
}
