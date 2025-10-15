package sample.service.output.jooq;

import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import sample.dto.CompanyDto;
import sample.service.output.OutputVariant;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Order(20)
public class JooqSqlOutputVariant implements OutputVariant {
    // language=sql
    private final static String SQL_LIST = """
            select c.company_id as "id", c.name as "name", c.industry as "industry", c.description as "description", c.url as "url", (select row (vi.username, vi.timestamp, vi.status, vi.comment) as "nested" from verification_info as "vi" where c.company_id = vi.company_id) as "verified", (select coalesce(jsonb_agg(jsonb_build_array("v0", "v1", "v2", "v3")), jsonb_build_array()) from (select o.office_id as "v0", o.name as "v1", o.address as "v2", o.city as "v3" from company_office as "o" where c.company_id = o.company_id order by o.name, o.office_id) as t) as "offices", (select coalesce(jsonb_agg(jsonb_build_array("v0", "v1", "v2", "v3")), jsonb_build_array()) from (select cp.contact_person_id as "v0", cp.name as "v1", cp.position as "v2", (select coalesce(jsonb_agg(jsonb_build_array("v0", "v1")), jsonb_build_array()) from (select cd.contact_type as "v0", cd.value as "v1" from contact_detail as "cd" where cd.contact_person_id = cp.contact_person_id order by cd.contact_type, cd.value) as t) as "v3" from contact_person as "cp" where c.company_id = cp.company_id order by cp.name, cp.contact_person_id) as t) as "contactPersons" from company as "c" where c.company_id = any (cast(? as uuid[])) order by c.name, c.company_id
            """;
    // language=sql
    private final static String SQL_SINGLE = """
            select c.company_id as "company_id", c.name as "name", c.industry as "industry", c.description as "description", c.url as "url", (select row (vi.username, vi.timestamp, vi.status, vi.comment) as "nested" from verification_info as "vi" where c.company_id = vi.company_id) as "verification_info", (select coalesce(jsonb_agg(jsonb_build_array("v0", "v1", "v2", "v3")), jsonb_build_array()) from (select o.office_id as "v0", o.name as "v1", o.address as "v2", o.city as "v3" from company_office as "o" where c.company_id = o.company_id order by o.name, o.office_id) as t) as "offices", (select coalesce(jsonb_agg(jsonb_build_array("v0", "v1", "v2", "v3")), jsonb_build_array()) from (select cp.contact_person_id as "v0", cp.name as "v1", cp.position as "v2", (select coalesce(jsonb_agg(jsonb_build_array("v0", "v1")), jsonb_build_array()) from (select cd.contact_type as "v0", cd.value as "v1" from contact_detail as "cd" where cd.contact_person_id = cp.contact_person_id order by cd.contact_type, cd.value) as t) as "v3" from contact_person as "cp" where c.company_id = cp.company_id order by cp.name, cp.contact_person_id) as t) as "contacts" from company as "c" where c.company_id = cast(? as uuid)
            """;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public String getName() {
        return "jOOQ (SQL only)";
    }

    @Override
    public List<CompanyDto> findList(List<UUID> companyIds) {
        return jdbcTemplate.query(SQL_LIST, ps ->
                        ps.setArray(1, ps.getConnection().createArrayOf("uuid", companyIds.toArray(UUID[]::new))),
                rs -> {
                    int n = rs.getMetaData().getColumnCount();
                    while (rs.next()) {
                        for (int i = 1; i <= n; i++) {
                            rs.getObject(i);
                        }
                    }
                    return List.of();
                });
    }

    @Override
    public Optional<CompanyDto> findSingle(UUID companyId) {
        return jdbcTemplate.query(SQL_SINGLE, ps ->
                        ps.setObject(1, companyId),
                rs -> {
                    int n = rs.getMetaData().getColumnCount();
                    while (rs.next()) {
                        for (int i = 1; i <= n; i++) {
                            rs.getObject(i);
                        }
                    }
                    return Optional.of(new CompanyDto());
                });
    }
}
