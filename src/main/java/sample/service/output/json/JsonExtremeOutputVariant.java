package sample.service.output.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import sample.dto.CompanyDto;
import sample.service.output.OutputVariant;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static sample.service.output.json.JsonOutputVariant.SELECT_JSON_ARRAY;
import static sample.service.output.json.JsonOutputVariant.SELECT_JSON_BY_ID;

@Service
@Slf4j
@RequiredArgsConstructor
@Order(15)
public class JsonExtremeOutputVariant implements OutputVariant {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public String getName() {
        return "JSON Extreme";
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
                    try {
                        return objectMapper.readValue(json, new TypeReference<>() {
                        });
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                },
                (Object) companyIds.toArray(UUID[]::new)
        );
    }

    @Override
    public Optional<CompanyDto> findSingle(UUID companyId) {
        return jdbcTemplate.query(SELECT_JSON_BY_ID, rs -> {
            if (!rs.next()) {
                return Optional.empty();
            }
            var json = rs.getString(1);
            try {
                var value = objectMapper.readValue(json, CompanyDto.class);
                return Optional.of(value);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }, companyId);
    }
}
