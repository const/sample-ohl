package sample.service.output.json;

import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import sample.dto.CompanyDto;
import sample.service.output.OutputVariant;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static sample.service.output.json.JsonOutputVariant.SELECT_JSON_BY_ID;

@Service
@RequiredArgsConstructor
@Order(17)
public class JsonSqlExtremeOutputVariant implements OutputVariant {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public String getName() {
        return "JSON Extreme (SQL only)";
    }

    @Override
    public boolean isSingleSupported() {
        return false;
    }

    @Override
    public List<CompanyDto> findList(List<UUID> companyIds) {
        return jdbcTemplate.query(JsonOutputVariant.SELECT_JSON_ARRAY,
                rs -> {
                    int n = rs.getMetaData().getColumnCount();
                    while (rs.next()) {
                        for (int i = 1; i <= n; i++) {
                            rs.getObject(i);
                        }
                    }
                    return List.of();
                },
                (Object) companyIds.toArray(UUID[]::new));
    }

    @Override
    public Optional<CompanyDto> findSingle(UUID companyId) {
        return jdbcTemplate.query(SELECT_JSON_BY_ID, rs -> {
            int n = rs.getMetaData().getColumnCount();
            while (rs.next()) {
                for (int i = 1; i <= n; i++) {
                    rs.getObject(i);
                }
            }
            return Optional.of(new CompanyDto());
        }, companyId);
    }
}
