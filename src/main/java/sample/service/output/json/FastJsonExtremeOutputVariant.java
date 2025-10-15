package sample.service.output.json;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import sample.dto.CompanyDto;
import sample.service.output.OutputVariant;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static sample.service.output.json.JsonOutputVariant.*;

@Service
@RequiredArgsConstructor
@Order(16)
public class FastJsonExtremeOutputVariant implements OutputVariant {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public String getName() {
        return "JSON Extreme (fastjson2)";
    }

    @Override
    public boolean isSingleSupported() {
        return false;
    }

    @Override
    public List<CompanyDto> findList(List<UUID> companyIds) {
        return jdbcTemplate.query(SELECT_JSON_ARRAY,
                rs -> {
                    if (!rs.next()) {
                        return List.of();
                    }
                    var json = rs.getString(1);
                    return JSON.parseObject(json,
                            new TypeReference<>() {});
                },
                (Object) companyIds.toArray(UUID[]::new));
    }

    @Override
    public Optional<CompanyDto> findSingle(UUID companyId) {
        return jdbcTemplate.query(SELECT_JSON_BY_ID, rs -> {
            if (!rs.next()) {
                return Optional.empty();
            }
            var json = rs.getString(1);
            var value = JSON.parseObject(json, CompanyDto.class);
            return Optional.of(value);
        }, companyId);
    }
}
