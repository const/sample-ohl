package sample.service.output.json;

import com.fasterxml.jackson.core.JsonProcessingException;
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

@Service
@Slf4j
@RequiredArgsConstructor
@Order(10)
public class JsonOutputVariant implements OutputVariant {
    // language=sql
    public static final String SELECT_JSON_BASE = """
            select
                json_build_object(
                    'id', c.company_id,
                    'name', c."name",
                    'industry', c.industry,
                    'description', c.description,
                    'url', c.url,
                    'verified', (
                        select
                            json_build_object(
                                'timestamp', vi."timestamp",
                                'status', vi.status,
                                'comment', vi."comment",
                                'user', vi.username
                            )
                        from verification_info vi where vi.company_id = c.company_id
                    ),
                    'contactPersons', (
                        select
                            coalesce(json_agg(
                                json_build_object(
                                    'id', cp.contact_person_id,
                                    'name', cp.name,
                                    'position', cp.position,
                                    'details', (
                                        select
                                            coalesce(json_agg(
                                                json_build_object(
                                                    'type', cd.contact_type,
                                                    'value', cd.value
                                                )
                                                order by cd.contact_type, cd.value
                                            ), json_build_array())
                                        from contact_detail cd
                                        where cd.contact_person_id = cp.contact_person_id
                                    )
                                )
                                order by cp.name, cp.contact_person_id
                            ), json_build_array())
                        from contact_person cp
                        where cp.company_id = c.company_id
                    ),
                    'offices', (
                        select
                            coalesce(json_agg(
                                json_build_object(
                                    'id', co.office_id,
                                    'name', co.name,
                                    'city', co.city,
                                    'address', co.address
                                )
                                order by co.name, co.office_id
                            ), json_build_array())
                        from company_office co
                        where co.company_id = c.company_id
                    )
                ) as company_dto
            """;
    // language=sql
    public static final String SELECT_JSON_BY_IDS = SELECT_JSON_BASE + "\nfrom company c where c.company_id = any(?)\norder by c.name, c.company_id";
    // language=sql
    public static final String SELECT_JSON_BY_ID = SELECT_JSON_BASE + "\nfrom company c where c.company_id = ?";
    // language=sql
    public static final String SELECT_JSON_ARRAY = """
            with company_data as (
            %s,
                    c.name,
                    c.company_id
                from company c where c.company_id = any(?)
            )
            select coalesce(json_agg(
                company_dto
                order by name, company_id
            ), json_build_array()) from company_data
            """.formatted(SELECT_JSON_BASE.indent(4));
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public String getName() {
        return "JSON";
    }

    @Override
    public List<CompanyDto> findList(List<UUID> companyIds) {
        return jdbcTemplate.query(SELECT_JSON_BY_IDS,
                (rs, i) -> {
                    var json = rs.getString(1);
                    try {
                        return objectMapper.readValue(json, CompanyDto.class);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
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
            try {
                var value = objectMapper.readValue(json, CompanyDto.class);
                return Optional.of(value);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }, companyId);
    }
}
