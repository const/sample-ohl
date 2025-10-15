package sample.service.output.layered;

import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import sample.dto.*;
import sample.model.ContactType;
import sample.model.VerificationStatus;
import sample.service.output.OutputVariant;

import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Order(1)
public class LayeredLeftJoinOutputVariant implements OutputVariant {
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Override
    public String getName() {
        return "Layered (LEFT JOIN)";
    }

    @Override
    public List<CompanyDto> findList(List<UUID> companyIds) {
        var params = new MapSqlParameterSource();
        params.addValue("companyIds", companyIds.toArray(UUID[]::new), Types.ARRAY);
        return findCompanies("where c.company_id = any (:companyIds)\norder by c.name, c.company_id", params);
    }

    @Override
    public Optional<CompanyDto> findSingle(UUID companyId) {
        var params = new MapSqlParameterSource();
        params.addValue("companyId", companyId);
        List<CompanyDto> companies = findCompanies("where c.company_id = :companyId", params);
        if (companies.size() > 1) {
            throw new IllegalStateException("Too much results: " + companies.size());
        }
        return companies.isEmpty() ? Optional.empty() : Optional.of(companies.getFirst());
    }

    List<CompanyDto> findCompanies(String querySuffix, MapSqlParameterSource params) {
        // this imitates typical DAO + Service in applications.
        var companies = findCompanyRoots(querySuffix, params);

        var keys = companies.stream()
                .map(CompanyDto::getId)
                .toArray(UUID[]::new);

        var officeResults = findOffices(keys);

        var officeByCompany = officeResults.stream().collect(
                Collectors.groupingBy(
                        Map.Entry::getKey,
                        Collectors.mapping(
                                Map.Entry::getValue,
                                Collectors.toList())));

        for (CompanyDto company : companies) {
            company.setOffices(
                    officeByCompany.getOrDefault(
                            company.getId(), List.of()));
        }

        var contactResults = findContacts(keys);
        var contactsByCompany = contactResults.stream().collect(
                Collectors.groupingBy(
                        Map.Entry::getKey,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
        for (CompanyDto company : companies) {
            company.setContactPersons(contactsByCompany.getOrDefault(company.getId(), List.of()));
        }
        var contactDetailResults = findContactDetails(contactResults.stream().map(e -> e.getValue().getId()).toArray(UUID[]::new));
        var contactDetailsByContact = contactDetailResults.stream().collect(
                Collectors.groupingBy(
                        Map.Entry::getKey,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
        for (Map.Entry<UUID, ContactPersonDto> contactResult : contactResults) {
            contactResult.getValue().setDetails(contactDetailsByContact.getOrDefault(contactResult.getValue().getId(), List.of()));
        }
        return companies;
    }

    private List<Map.Entry<UUID, ContactDetailsDto>> findContactDetails
            (UUID[] contactIds) {
        // language=sql
        var contactDetailSql = """
                select cd1_0.contact_person_id,cd1_0.contact_type,cd1_0.value
                from contact_detail cd1_0
                where cd1_0.contact_person_id = any (:contactPersonIds)
                order by cd1_0.contact_person_id,cd1_0.contact_type,cd1_0.value
                """;
        return namedParameterJdbcTemplate.query(contactDetailSql,
                Map.of("contactPersonIds", contactIds),
                (rs, i) -> Map.entry(
                        rs.getObject(1, UUID.class),
                        ContactDetailsDto.builder()
                                .type(ContactType.valueOf(rs.getString(2)))
                                .value(rs.getString(3))
                                .build()
                ));
    }

    private List<Map.Entry<UUID, ContactPersonDto>> findContacts(UUID[] keys) {
        String contactsSql;
        Map<String, ?> contactParams;
        if (keys.length != 1) {
            // language=sql
            contactsSql = """
                    select cp1_0.company_id,cp1_0.contact_person_id,cp1_0.name,cp1_0.position
                    from contact_person cp1_0 where cp1_0.company_id = any (:companyIds)
                    order by cp1_0.company_id, cp1_0.name, cp1_0.contact_person_id
                    """;
            contactParams = Map.of("companyIds", keys);
        } else {
            // language=sql
            contactsSql = """
                    select cp1_0.company_id,cp1_0.contact_person_id,cp1_0.name,cp1_0.position
                    from contact_person cp1_0 where cp1_0.company_id = :companyId
                    order by cp1_0.company_id, cp1_0.name, cp1_0.contact_person_id
                    """;
            contactParams = Map.of("companyId", keys[0]);
        }
        return namedParameterJdbcTemplate.query(contactsSql,
                contactParams,
                (rs, i) -> Map.entry(
                        rs.getObject(1, UUID.class),
                        ContactPersonDto.builder()
                                .id(rs.getObject(2, UUID.class))
                                .name(rs.getString(3))
                                .position(rs.getString(4))
                                .build()
                ));
    }

    private List<Map.Entry<UUID, OfficeDto>> findOffices(UUID[] keys) {
        String officeSql;
        Map<String, ?> officeParams;
        if (keys.length != 1) {
            // language=sql
            officeSql = """
                    select o.company_id,o.office_id,o.address,o.city,o.name
                    from company_office o where o.company_id = any (:companyIds)
                    order by o.company_id,o.name,o.office_id
                    """;
            officeParams = Map.of("companyIds", keys);
        } else {
            // language=sql
            officeSql = """
                    select o.company_id,o.office_id,o.address,o.city,o.name
                    from company_office o where o.company_id = :companyId
                    order by o.company_id,o.name,o.office_id
                    """;
            officeParams = Map.of("companyId", keys[0]);
        }
        return namedParameterJdbcTemplate.query(officeSql, officeParams,
                (rs, i) -> Map.entry(
                        rs.getObject(1, UUID.class),
                        OfficeDto.builder()
                                .id(rs.getObject(2, UUID.class))
                                .address(rs.getString(3))
                                .city(rs.getString(4))
                                .name(rs.getString(5))
                                .build()
                ));
    }

    private List<CompanyDto> findCompanyRoots(String querySuffix, MapSqlParameterSource params) {
        // language=sql
        var sql = """
                select
                    c.company_id, c.description, c.industry,c.name, c.url,
                    vi.company_id, vi.comment, vi.status, vi.timestamp, vi.username
                from company c
                left join verification_info vi on vi.company_id = c.company_id
                """ + querySuffix;
        return namedParameterJdbcTemplate.query(sql, params, (rs, i) -> CompanyDto.builder()
                .id(rs.getObject(1, UUID.class))
                .description(rs.getString(2))
                .industry(rs.getString(3))
                .name(rs.getString(4))
                .url(rs.getString(5))
                .verified(rs.getObject(6, UUID.class) == null ? null : VerificationInfoDto.builder()
                        .comment(rs.getString(7))
                        .status(VerificationStatus.valueOf(rs.getString(8)))
                        .timestamp(rs.getObject(9, OffsetDateTime.class).toInstant())
                        .user(rs.getString(10))
                        .build())
                .build());
    }
}
