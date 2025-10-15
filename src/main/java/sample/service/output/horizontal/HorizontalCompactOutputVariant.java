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
import java.util.*;

@Component
@RequiredArgsConstructor
@Order(31)
public class HorizontalCompactOutputVariant implements OutputVariant {
    private final JdbcTemplate jdbcTemplate;
    // language=sql
    private static final String SELECT_BY_IDS = """
            with company_data as (
                select
                    row_number() over (order by c.name, c.company_id) company_rn,
                    c.company_id as c_company_id,
                    c.description as c_description,
                    c.industry as c_industry,
                    c."name" as c_name,
                    c.url as c_url
                from company c
                where company_id = any(?)
            ), company_office_data as (
                select
                    row_number() over (order by cd.company_rn, co.name, co.office_id) office_rn,
                    co.company_id as o_company_id,
                    co.office_id as o_office_id,
                    co.address as o_address,
                    co.city as o_city,
                    co."name" as o_name
                from company_data cd
                join company_office co on co.company_id = cd.c_company_id
            ), contact_person_data as (
                select
                    row_number() over (
                        order by cd.company_rn, cp.name, cp.contact_person_id
                    ) contact_person_rn,
                    cp.company_id as p_company_id,
                    cp.contact_person_id as p_contact_person_id,
                    cp."name" as p_name,
                    cp."position" as p_position
                from company_data cd
                join contact_person cp on cp.company_id = cd.c_company_id
            ), contact_detail_data as (
                select
                    row_number() over (
                        order by cpd.contact_person_rn, cd.contact_type, cd.value
                    ) contact_detail_rn,
                    cd.contact_person_id as d_contact_person_id,
                    cd.contact_type as d_contact_type,
                    cd.value as d_value
                from contact_person_data cpd
                join contact_detail cd on cpd.p_contact_person_id = cd.contact_person_id
            )
            select
                -- company
                cd.c_company_id, cd.c_description, cd.c_industry, cd.c_name, cd.c_url,
                -- verification_info
                vi.company_id as vi_company_id, vi."comment" as vi_comment, vi.status as vi_status,
                vi."timestamp" as vi_timestamp, vi.username as vi_username,
                -- company_office
                cod.o_company_id, cod.o_office_id, cod.o_address, cod.o_city, cod.o_name,
                -- contact_person
                cpd.p_company_id, cpd.p_contact_person_id, cpd.p_name, cpd.p_position,
                -- contact_detail
                cdd.d_contact_person_id, cdd.d_contact_type, cdd.d_value
            from company_data cd
            left join verification_info vi on cd.c_company_id = vi.company_id
            full outer join company_office_data cod on cd.company_rn = cod.office_rn
            full outer join contact_person_data cpd
                on coalesce(cd.company_rn,cod.office_rn) = cpd.contact_person_rn
            full outer join contact_detail_data cdd
                on coalesce(cd.company_rn,cod.office_rn,cpd.contact_person_rn) = cdd.contact_detail_rn
            order by coalesce(cd.company_rn, cod.office_rn, cpd.contact_person_rn, cdd.contact_detail_rn)
            """;

    @Override
    public String getName() {
        return "Horizontal (Compact)";
    }

    @Override
    public List<CompanyDto> findList(List<UUID> companyIds) {
        var params = companyIds.toArray(UUID[]::new);
        var resultSetReader = new ResultSetExtractor<List<CompanyDto>>() {
            @Override
            public List<CompanyDto> extractData(@NonNull ResultSet rs) throws SQLException, DataAccessException {
                List<CompanyDto> result = new ArrayList<>();
                Map<UUID, List<OfficeDto>> offices = new HashMap<>();
                Map<UUID, List<ContactPersonDto>> contacts = new HashMap<>();
                Map<UUID, List<ContactDetailsDto>> contactDetails = new HashMap<>();
                int[] current = new int[1];
                // gather
                while (rs.next()) {
                    current[0] = 1;
                    readCompany(rs, current, result);
                    readOffice(rs, current, offices);
                    readContact(rs, current, contacts);
                    readContactDetail(rs, current, contactDetails);
                }
                // link
                for (CompanyDto companyDto : result) {
                    companyDto.setOffices(
                            offices.getOrDefault(companyDto.getId(), List.of()));
                    companyDto.setContactPersons(
                            contacts.getOrDefault(companyDto.getId(), List.of()));
                    for (ContactPersonDto contactPerson : companyDto.getContactPersons()) {
                        contactPerson.setDetails(
                                contactDetails.getOrDefault(contactPerson.getId(),
                                        List.of()));
                    }
                }
                return result;
            }

            private static void readContactDetail(ResultSet rs, int[] current, Map<UUID, List<ContactDetailsDto>> contactDetails) throws SQLException {
                var contactId = rs.getObject(current[0]++, UUID.class);
                if (contactId != null) {
                    var contactDetail = new ContactDetailsDto();
                    contactDetails.computeIfAbsent(contactId, k -> new ArrayList<>()).add(contactDetail);
                    contactDetail.setType(ContactType.valueOf(rs.getString(current[0]++)));
                    contactDetail.setValue(rs.getString(current[0]++));
                } else {
                    current[0] += 2;
                }

            }

            private static void readContact(ResultSet rs, int[] current, Map<UUID, List<ContactPersonDto>> contacts) throws SQLException {
                var companyId = rs.getObject(current[0]++, UUID.class);
                if (companyId != null) {
                    var contact = new ContactPersonDto();
                    contacts.computeIfAbsent(companyId, k -> new ArrayList<>()).add(contact);
                    contact.setId(rs.getObject(current[0]++, UUID.class));
                    contact.setName(rs.getString(current[0]++));
                    contact.setPosition(rs.getString(current[0]++));
                } else {
                    current[0] += 3;
                }
            }

            private static void readOffice(
                    ResultSet rs, int[] current,
                    Map<UUID, List<OfficeDto>> offices) throws SQLException {
                var companyId = rs.getObject(current[0]++, UUID.class);
                if (companyId != null) {
                    var office = new OfficeDto();
                    offices.computeIfAbsent(companyId,
                            k -> new ArrayList<>()).add(office);
                    office.setId(rs.getObject(current[0]++, UUID.class));
                    office.setAddress(rs.getString(current[0]++));
                    office.setCity(rs.getString(current[0]++));
                    office.setName(rs.getString(current[0]++));
                } else {
                    current[0] += 4;
                }
            }

            private static void readCompany(ResultSet rs, int[] current, List<CompanyDto> result)
                    throws SQLException {
                var companyId = rs.getObject(current[0]++, UUID.class);
                CompanyDto company = null;
                if (companyId != null) {
                    company = new CompanyDto();
                    result.add(company);
                    company.setId(companyId);
                    company.setDescription(rs.getString(current[0]++));
                    company.setIndustry(rs.getString(current[0]++));
                    company.setName(rs.getString(current[0]++));
                    company.setUrl(rs.getString(current[0]++));
                } else {
                    current[0] += 4;
                }
                readVerificationInfo(rs, current, company);
            }

            private static void readVerificationInfo(ResultSet rs, int[] current, CompanyDto company) throws SQLException {
                var verificationId = rs.getObject(current[0]++, UUID.class);
                if (verificationId != null) {
                    var verificationInfo = new VerificationInfoDto();
                    company.setVerified(verificationInfo);
                    verificationInfo.setComment(rs.getString(current[0]++));
                    var status = rs.getString(current[0]++);
                    verificationInfo.setStatus(status == null ? null : VerificationStatus.valueOf(status));
                    var timestamp = rs.getObject(current[0]++, OffsetDateTime.class);
                    verificationInfo.setTimestamp(timestamp == null ? null : timestamp.toInstant());
                    verificationInfo.setUser(rs.getString(current[0]++));
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
