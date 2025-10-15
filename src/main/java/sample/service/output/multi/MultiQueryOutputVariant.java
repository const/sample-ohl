package sample.service.output.multi;

import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import sample.dto.*;
import sample.model.ContactType;
import sample.model.VerificationStatus;
import sample.service.output.OutputVariant;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Order(80)
@RequiredArgsConstructor
public class MultiQueryOutputVariant implements OutputVariant {
    private final NamedParameterJdbcTemplate jdbcTemplate;
    // language=sql
    private static final String QUERY_SINGLE = """
            select c.company_id, c.description, c.industry,c.name, c.url
            from company c where c.company_id = :companyId;
            
            select vi.company_id,vi.comment,vi.status,vi.timestamp,vi.username
            from verification_info vi where vi.company_id = :companyId;
            
            select o.company_id,o.office_id,o.address,o.city,o.name
            from company_office o where o.company_id = :companyId
            order by o.company_id,o.name,o.office_id;
            
            select cp1_0.company_id,cp1_0.contact_person_id,cp1_0.name,cp1_0.position
            from contact_person cp1_0 where cp1_0.company_id = :companyId
            order by cp1_0.company_id,cp1_0.name,cp1_0.contact_person_id;
            
            select cd1_0.contact_person_id,cd1_0.contact_type,cd1_0.value
            from contact_person cp
            join contact_detail cd1_0 on cd1_0.contact_person_id = cp.contact_person_id
            where cp.company_id = :companyId
            order by cd1_0.contact_person_id,cd1_0.contact_type,cd1_0.value;
            """;

    // language=sql
    private static final String QUERY_LIST = """
            select c.company_id, c.description, c.industry,c.name, c.url
            from company c where c.company_id = any (:companyIds)
            order by c.name, c.company_id;
            
            select vi.company_id,vi.comment,vi.status,vi.timestamp,vi.username
            from verification_info vi where vi.company_id = any(:companyIds);
            
            select o.company_id,o.office_id,o.address,o.city,o.name
            from company_office o where o.company_id = any (:companyIds)
            order by o.company_id,o.name,o.office_id;
            
            select cp1_0.company_id,cp1_0.contact_person_id,cp1_0.name,cp1_0.position
            from contact_person cp1_0 where cp1_0.company_id = any (:companyIds)
            order by cp1_0.company_id,cp1_0.name,cp1_0.contact_person_id;
            
            select cd1_0.contact_person_id,cd1_0.contact_type,cd1_0.value
            from contact_person cp
            join contact_detail cd1_0 on cd1_0.contact_person_id = cp.contact_person_id
            where cp.company_id = any (:companyIds)
            order by cd1_0.contact_person_id,cd1_0.contact_type,cd1_0.value;
            """;


    @Override
    public String getName() {
        return "Multi-Query (By ID)";
    }

    @Override
    public List<CompanyDto> findList(List<UUID> companyIds) {
        return findCompanies(QUERY_LIST, Map.of("companyIds", companyIds.toArray(UUID[]::new)));
    }

    @Override
    public Optional<CompanyDto> findSingle(UUID companyId) {
        var companies = findCompanies(QUERY_SINGLE, Map.of("companyId", companyId));
        return companies.isEmpty() ? Optional.empty() : Optional.of(companies.getFirst());
    }

    List<CompanyDto> findCompanies(String sql, Map<String, ?> parameters) {
        return jdbcTemplate.execute(sql, parameters, ps -> {
            List<CompanyDto> companies;
            // execute and check if ResultSet
            if (!ps.execute()) { throw new RuntimeException("No result set"); }
            try (ResultSet rs = ps.getResultSet()) {
                companies = findCompanyRoots(rs);
            }
            var companyById = companies.stream()
                    .collect(Collectors.toMap(
                            CompanyDto::getId, Function.identity()));
            try (ResultSet rs = nextResultSet(ps)) {
                var verificationResults = findVerifications(rs);
                for (Map.Entry<UUID, VerificationInfoDto> verificationResult
                        : verificationResults) {
                    companyById.get(verificationResult.getKey())
                            .setVerified(verificationResult.getValue());
                }
            }
            // ...
            try (ResultSet rs = nextResultSet(ps)) {
                var officeResults = findOffices(rs);
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
            }
            List<Map.Entry<UUID, ContactPersonDto>> contactResults;
            try (ResultSet rs = nextResultSet(ps)) {
                contactResults = findContacts(rs);
                var contactsByCompany = contactResults.stream().collect(
                        Collectors.groupingBy(
                                Map.Entry::getKey,
                                Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
                for (CompanyDto company : companies) {
                    company.setContactPersons(contactsByCompany.getOrDefault(company.getId(), List.of()));
                }
            }
            try (ResultSet rs = nextResultSet(ps)) {
                var contactDetailResults = findContactDetails(rs);
                var contactDetailsByContact = contactDetailResults.stream().collect(
                        Collectors.groupingBy(
                                Map.Entry::getKey,
                                Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
                for (Map.Entry<UUID, ContactPersonDto> contactResult : contactResults) {
                    contactResult.getValue().setDetails(contactDetailsByContact.getOrDefault(contactResult.getValue().getId(), List.of()));
                }
            }
            return companies;
        });
    }
    // next results
    private ResultSet nextResultSet(PreparedStatement ps) throws SQLException {
        if (!ps.getMoreResults(Statement.CLOSE_CURRENT_RESULT)) {
            throw new RuntimeException("No more results");
        }
        return ps.getResultSet();
    }

    private List<Map.Entry<UUID, ContactDetailsDto>> findContactDetails(ResultSet rs) throws SQLException {
        var list = new ArrayList<Map.Entry<UUID, ContactDetailsDto>>();
        while (rs.next()) {
            list.add(Map.entry(
                    rs.getObject(1, UUID.class),
                    ContactDetailsDto.builder()
                            .type(ContactType.valueOf(rs.getString(2)))
                            .value(rs.getString(3))
                            .build()));
        }
        return list;
    }

    private List<Map.Entry<UUID, ContactPersonDto>> findContacts(ResultSet rs) throws SQLException {
        var list = new ArrayList<Map.Entry<UUID, ContactPersonDto>>();
        while (rs.next()) {
            list.add(Map.entry(
                    rs.getObject(1, UUID.class),
                    ContactPersonDto.builder()
                            .id(rs.getObject(2, UUID.class))
                            .name(rs.getString(3))
                            .position(rs.getString(4))
                            .build()));
        }
        return list;
    }

    private List<Map.Entry<UUID, OfficeDto>> findOffices(ResultSet rs) throws SQLException {
        var list = new ArrayList<Map.Entry<UUID, OfficeDto>>();
        while (rs.next()) {
            list.add(Map.entry(
                    rs.getObject(1, UUID.class),
                    OfficeDto.builder()
                            .id(rs.getObject(2, UUID.class))
                            .address(rs.getString(3))
                            .city(rs.getString(4))
                            .name(rs.getString(5))
                            .build()));
        }
        return list;
    }

    private List<Map.Entry<UUID, VerificationInfoDto>> findVerifications(ResultSet rs) throws SQLException {
        var list = new ArrayList<Map.Entry<UUID, VerificationInfoDto>>();
        while (rs.next()) {
            list.add(Map.entry(
                    rs.getObject(1, UUID.class),
                    VerificationInfoDto.builder()
                            .comment(rs.getString(2))
                            .status(VerificationStatus.valueOf(rs.getString(3)))
                            .timestamp(rs.getObject(4, OffsetDateTime.class).toInstant())
                            .user(rs.getString(5))
                            .build()));
        }
        return list;
    }

    private List<CompanyDto> findCompanyRoots(ResultSet rs) throws SQLException {
        var list = new ArrayList<CompanyDto>();
        while (rs.next()) {
            list.add(CompanyDto.builder()
                    .id(rs.getObject(1, UUID.class))
                    .description(rs.getString(2))
                    .industry(rs.getString(3))
                    .name(rs.getString(4))
                    .url(rs.getString(5))
                    .build());
        }
        return list;
    }

}
