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

@Component
@RequiredArgsConstructor
@Order(32)
public class HorizontalStreamDOutputVariant implements OutputVariant {
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
                        order by co.name, co.office_id
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
                        order by cd.contact_type, cd.value
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
                        order by coalesce(cpd.level2, cdd.level2),
                            coalesce(cdd.level3, cpd.level3)
                    ) as level2,
                    cpd.p_contact_person_id, cpd.p_name, cpd.p_position,
                    cdd.d_contact_type, cdd.d_value
                from contact_person_data cpd
                full outer join contact_detail_data cdd
                    on cpd.level1 = cdd.level1 and cpd.level2 = cdd.level2 and cdd.level3 = cpd.level3
            )
            select
                -- company
                cd.c_company_id, cd.c_description, cd.c_industry, cd.c_name, cd.c_url,
                -- verification_info
                vi.company_id as vi_company_id, vi."comment" as vi_comment, vi.status as vi_status,
                vi."timestamp" as vi_timestamp, vi.username as vi_username,
                -- company_office
                cod.o_office_id, cod.o_address, cod.o_city, cod.o_name,
                -- contact_person
                cpd.p_contact_person_id, cpd.p_name, cpd.p_position,
                -- contact_detail
                cpd.d_contact_type, cpd.d_value
            from company_data cd
            left join verification_info vi on cd.c_company_id = vi.company_id
            full outer join company_office_data cod
                on cd.level1 = cod.level1 and cod.level2 = cd.level2
            full outer join contact_person_detail_data cpd
                on coalesce(cd.level1, cod.level1) = cpd.level1
                and coalesce(cod.level2, cd.level2) = cpd.level2
            order by coalesce(cd.level1, cod.level1, cpd.level1),
                coalesce(cd.level2, cod.level2, cpd.level2, 1)
            """;

    @Override
    public String getName() {
        return "Horizontal (Stream D)";
    }

    @Override
    public List<CompanyDto> findList(List<UUID> companyIds) {
        var params = companyIds.toArray(UUID[]::new);
        var resultSetReader = new ResultSetExtractor<List<CompanyDto>>() {
            @Override
            public List<CompanyDto> extractData(@NonNull ResultSet rs)
                    throws SQLException, DataAccessException {
                List<CompanyDto> result = new ArrayList<>();
                CompanyDto companyDto = null;
                ContactPersonDto contactPersonDto = null;
                // gather
                int[] current = new int[1];
                while (rs.next()) {
                    current[0] = 1;
                    companyDto = readCompany(rs, current, result, companyDto);
                    readVerificationInfo(rs, current, companyDto);
                    readOffice(rs, current, companyDto);
                    contactPersonDto = readContact(rs, current,
                            companyDto, contactPersonDto);
                    readContactDetail(rs, current, contactPersonDto);
                }
                return result;
            }

            private static void readContactDetail(ResultSet rs, int[] current, ContactPersonDto contactPersonDto) throws SQLException {
                String contactType = rs.getString(current[0]++);
                if (contactType != null) {
                    var contactDetail = new ContactDetailsDto();
                    contactDetail.setType(ContactType.valueOf(contactType));
                    contactDetail.setValue(rs.getString(current[0]++));
                    contactPersonDto.getDetails().add(contactDetail);
                } else {
                    current[0]++;
                }
            }

            private static ContactPersonDto readContact(ResultSet rs, int[] current,
                    CompanyDto companyDto, ContactPersonDto previous)
                    throws SQLException {
                UUID contactId = rs.getObject(current[0]++, UUID.class);
                if (contactId != null) {
                    var contact = new ContactPersonDto();
                    contact.setId(contactId);
                    contact.setName(rs.getString(current[0]++));
                    contact.setPosition(rs.getString(current[0]++));
                    contact.setDetails(new ArrayList<>());
                    companyDto.getContactPersons().add(contact);
                    return contact;
                } else {
                    current[0] += 2;
                    return previous;
                }
            }

            private static void readOffice(ResultSet rs, int[] current, CompanyDto companyDto) throws SQLException {
                UUID officeId = rs.getObject(current[0]++, UUID.class);
                if (officeId != null) {
                    var office = new OfficeDto();
                    office.setId(officeId);
                    office.setAddress(rs.getString(current[0]++));
                    office.setCity(rs.getString(current[0]++));
                    office.setName(rs.getString(current[0]++));
                    companyDto.getOffices().add(office);
                } else {
                    current[0] += 3;
                }
            }

            private static CompanyDto readCompany(ResultSet rs, int[] current, List<CompanyDto> result, CompanyDto previous) throws SQLException {
                var companyId = rs.getObject(current[0]++, UUID.class);
                if (companyId != null) {
                    var company = new CompanyDto();
                    company.setId(companyId);
                    company.setDescription(rs.getString(current[0]++));
                    company.setIndustry(rs.getString(current[0]++));
                    company.setName(rs.getString(current[0]++));
                    company.setUrl(rs.getString(current[0]++));
                    company.setOffices(new ArrayList<>());
                    company.setContactPersons(new ArrayList<>());
                    result.add(company);
                    return company;
                } else {
                    current[0] += 4;
                    return previous;
                }
            }

            private static void readVerificationInfo(ResultSet rs, int[] current, CompanyDto companyDto) throws SQLException {
                var verificationId = rs.getObject(current[0]++, UUID.class);
                if (verificationId != null) {
                    var verificationInfo = new VerificationInfoDto();
                    verificationInfo.setComment(rs.getString(current[0]++));
                    var status = rs.getString(current[0]++);
                    verificationInfo.setStatus(status == null ? null : VerificationStatus.valueOf(status));
                    var timestamp = rs.getObject(current[0]++, OffsetDateTime.class);
                    verificationInfo.setTimestamp(timestamp == null ? null : timestamp.toInstant());
                    verificationInfo.setUser(rs.getString(current[0]++));
                    companyDto.setVerified(verificationInfo);
                } else {
                    current[0] += 4;
                }
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
