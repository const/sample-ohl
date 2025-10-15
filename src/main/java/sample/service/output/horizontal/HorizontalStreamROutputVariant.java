package sample.service.output.horizontal;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Component;
import sample.dto.*;
import sample.model.ContactType;
import sample.model.VerificationStatus;
import sample.service.output.OutputVariant;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
@Order(33)
public class HorizontalStreamROutputVariant implements OutputVariant {
    private final JdbcTemplate jdbcTemplate;
    // language=sql
    private static final String SELECT_BY_IDS = """
            with company_data as (
                select
                    row_number() over (order by c.name, c.company_id) level1,
                    1 level2,
                    c.company_id as c_company_id,
                    c.description as c_description,
                    c.industry as c_industry,
                    c."name" as c_name,
                    c.url as c_url
                from company c
                where company_id = any(?)
            ), company_office_data as (
                select
                    cd.level1,
                    row_number() over (
                        partition by cd.level1
                        order by co.name desc, co.office_id desc
                    ) as level2,
                    co.company_id as o_company_id,
                    co.office_id as o_office_id,
                    co.address as o_address,
                    co.city as o_city,
                    co."name" as o_name
                from company_data cd
                join company_office co on co.company_id = cd.c_company_id
            ), contact_person_data as (
                select
                    cd.level1,
                    row_number() over (
                        partition by cd.level1
                        order by cp.name, cp.contact_person_id
                    ) as level2,
                    1 level3,
                    cp.contact_person_id as p_contact_person_id,
                    cp."name" as p_name,
                    cp."position" as p_position
                from company_data cd
                join contact_person cp on cp.company_id = cd.c_company_id
            ), contact_detail_data as (
                select
                    cpd.level1,
                    cpd.level2,
                    row_number() over (
                        partition by cpd.level1, cpd.level2
                        order by cd.contact_type desc, cd.value desc
                    ) level3,
                    cd.contact_type as d_contact_type,
                    cd.value as d_value
                from contact_person_data cpd
                join contact_detail cd on cpd.p_contact_person_id = cd.contact_person_id
            ), contact_person_detail_data as (
                select
                    coalesce(cpd.level1, cdd.level1) as level1,
                    row_number() over (
                        partition by coalesce(cpd.level1, cdd.level1)
                        order by coalesce(cpd.level2, cdd.level2) desc,
                            coalesce(cdd.level3, cpd.level3)
                    ) as level2,
                    cdd.d_contact_type, cdd.d_value,
                    cpd.p_contact_person_id, cpd.p_name, cpd.p_position
                from contact_person_data cpd
                full outer join contact_detail_data cdd
                    on cpd.level1 = cdd.level1 and cpd.level2 = cdd.level2 and cdd.level3 = cpd.level3
            )
            select
                -- contact_detail
                cpd.d_contact_type, cpd.d_value,
                -- contact_person
                cpd.p_contact_person_id, cpd.p_name, cpd.p_position,
                -- company_office
                cod.o_office_id, cod.o_address, cod.o_city, cod.o_name,
                -- verification_info
                vi.company_id as vi_company_id, vi."comment" as vi_comment, vi.status as vi_status,
                vi."timestamp" as vi_timestamp, vi.username as vi_username,
                -- company
                cd.c_company_id, cd.c_description, cd.c_industry, cd.c_name, cd.c_url
            from company_data cd
            left join verification_info vi on cd.c_company_id = vi.company_id
            full outer join company_office_data cod
                on cd.level1 = cod.level1 and cod.level2 = cd.level2
            full outer join contact_person_detail_data cpd
                on coalesce(cd.level1, cod.level1) = cpd.level1
                and coalesce(cod.level2, cd.level2) = cpd.level2
            order by coalesce(cd.level1, cod.level1, cpd.level1),
                coalesce(cd.level2, cod.level2, cpd.level2, 1) desc
            """;

    @Override
    public String getName() {
        return "Horizontal (Stream R)";
    }

    @Override
    public List<CompanyDto> findList(List<UUID> companyIds) {
        var params = companyIds.toArray(UUID[]::new);
        var resultSetReader = new ResultSetExtractor<List<CompanyDto>>() {
            @Override
            public List<CompanyDto> extractData(@NonNull ResultSet rs) throws SQLException, DataAccessException {
                List<CompanyDto> result = new ArrayList<>();
                List<OfficeDto> offices = new ArrayList<>();
                List<ContactPersonDto> contacts = new ArrayList<>();
                List<ContactDetailsDto> contactDetails = new ArrayList<>();
                // gather
                int[] current = new int[1];
                while (rs.next()) {
                    current[0] = 1;
                    readContactDetail(rs, current, contactDetails);
                    if (readContact(rs, current, contacts, contactDetails)) {
                        contactDetails = new ArrayList<>();
                    }
                    readOffice(rs, current, offices);
                    if (readCompany(rs, current, result, offices, contacts)) {
                        offices = new ArrayList<>();
                        contacts = new ArrayList<>();
                    }
                }
                return result;
            }

            private static void readContactDetail(ResultSet rs, int[] current, List<ContactDetailsDto> contactDetails) throws SQLException {
                String contactType = rs.getString(current[0]++);
                if (contactType != null) {
                    contactDetails.add(ContactDetailsDto.builder()
                            .type(ContactType.valueOf(contactType))
                            .value(rs.getString(current[0]++))
                            .build());
                } else {
                    current[0]++;
                }
            }

            private static boolean readContact(ResultSet rs, int[] current, List<ContactPersonDto> contacts, List<ContactDetailsDto> contactDetails) throws SQLException {
                UUID contactId = rs.getObject(current[0]++, UUID.class);
                if (contactId != null) {
                    contacts.add(ContactPersonDto.builder()
                            .id(contactId)
                            .name(rs.getString(current[0]++))
                            .position(rs.getString(current[0]++))
                            .details(contactDetails)
                            .build());
                    return true;
                } else {
                    current[0] += 2;
                }
                return false;
            }

            private static void readOffice(ResultSet rs, int[] current, List<OfficeDto> offices) throws SQLException {
                UUID officeId = rs.getObject(current[0]++, UUID.class);
                if (officeId != null) {
                    offices.add(OfficeDto.builder()
                            .id(officeId)
                            .address(rs.getString(current[0]++))
                            .city(rs.getString(current[0]++))
                            .name(rs.getString(current[0]++))
                            .build());
                } else {
                    current[0] += 3;
                }
            }

            private static boolean readCompany(ResultSet rs, int[] current, List<CompanyDto> result, List<OfficeDto> offices, List<ContactPersonDto> contacts) throws SQLException {
                var verificationInfo = readVerificationInfo(rs, current);
                var companyId = rs.getObject(current[0]++, UUID.class);
                if (companyId != null) {
                    result.add(CompanyDto.builder()
                            .id(companyId)
                            .description(rs.getString(current[0]++))
                            .industry(rs.getString(current[0]++))
                            .name(rs.getString(current[0]++))
                            .url(rs.getString(current[0]++))
                            .verified(verificationInfo)
                            .offices(offices)
                            .contactPersons(contacts)
                            .build());
                    return true;
                } else {
                    current[0] += 4;
                }
                return false;
            }

            private static VerificationInfoDto readVerificationInfo(ResultSet rs, int[] current) throws SQLException {
                var verificationId = rs.getObject(current[0]++, UUID.class);
                if (verificationId != null) {
                    return VerificationInfoDto.builder()
                            .comment(rs.getString(current[0]++))
                            .status(getNullable(rs.getString(current[0]++), VerificationStatus::valueOf))
                            .timestamp(getNullable(rs.getObject(current[0]++, OffsetDateTime.class), OffsetDateTime::toInstant))
                            .user(rs.getString(current[0]++))
                            .build();
                } else {
                    current[0] += 4;
                    return null;
                }
            }

            private static <T, R> R getNullable(T value, Function<T, R> converter) {
                return value == null ? null : converter.apply(value);
            }
        };
        return jdbcTemplate.query(SELECT_BY_IDS, resultSetReader, (Object) params);
    }

    @Override
    public Optional<CompanyDto> findSingle(UUID companyId) {
        var result = findList(List.of(companyId));
        if (result.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(result.getFirst());
        }
    }
}
