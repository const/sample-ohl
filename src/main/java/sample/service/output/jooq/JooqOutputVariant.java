package sample.service.output.jooq;

import lombok.RequiredArgsConstructor;
import org.jooq.*;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import sample.dto.*;
import sample.model.ContactType;
import sample.model.VerificationStatus;
import sample.service.output.OutputVariant;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.jooq.impl.DSL.*;

@Service
@RequiredArgsConstructor
@Order(21)
public class JooqOutputVariant implements OutputVariant {

    private final DSLContext create;

    @Override
    public String getName() {
        return "jOOQ";
    }

    @Override
    public List<CompanyDto> findList(List<UUID> companyIds) {
        var select = getBaseSelect()
                .where(field("c.company_id", UUID.class).eq(any(companyIds.toArray(UUID[]::new))))
                .orderBy(field("c.name", String.class), field("c.company_id", UUID.class));
        return select.fetch(mapper());
    }

    @Override
    public Optional<CompanyDto> findSingle(UUID companyId) {
        var select = getBaseSelect()
                .where(field("c.company_id", UUID.class).eq(companyId));
        return select.fetchOptional(mapper());
    }

    private static RecordMapper<Record8<UUID, String, String, String, String, Record4<String, OffsetDateTime, String, String>, List<OfficeDto>, List<ContactPersonDto>>, CompanyDto> mapper() {
        return r -> CompanyDto.builder()
                .id(r.value1())
                .name(r.value2())
                .industry(r.value3())
                .description(r.value4())
                .url(r.value5())
                .verified(r.value6() == null ? null : VerificationInfoDto.builder()
                        .user(r.value6().value1())
                        .timestamp(r.value6().value2().toInstant())
                        .status(VerificationStatus.valueOf(r.value6().value3()))
                        .comment(r.value6().value4())
                        .build())
                .offices(r.value7())
                .contactPersons(r.value8())
                .build();
    }

    private SelectJoinStep<Record8<UUID, String, String, String, String, Record4<String, OffsetDateTime, String, String>, List<OfficeDto>, List<ContactPersonDto>>> getBaseSelect() {
        return create.select(
                        field("c.company_id", UUID.class).as("company_id"),
                        field("c.name", String.class).as("name"),
                        field("c.industry", String.class).as("industry"),
                        field("c.description", String.class).as("description"),
                        field("c.url", String.class).as("url"),
                        field(
                                select(
                                        row(
                                                field("vi.username", String.class),
                                                field("vi.timestamp", OffsetDateTime.class),
                                                field("vi.status", String.class),
                                                field("vi.comment", String.class)
                                        )
                                ).from(table("verification_info").as("vi"))
                                        .where(field("c.company_id").eq(field("vi.company_id")))
                        ).as("verification_info"),
                        multiset(
                                select(
                                        field("o.office_id", UUID.class),
                                        field("o.name", String.class),
                                        field("o.address", String.class),
                                        field("o.city", String.class)
                                ).from(table("company_office").as("o"))
                                        .where(field("c.company_id").eq(field("o.company_id")))
                                        .orderBy(field("o.name", String.class), field("o.office_id", UUID.class))
                        ).as("offices").convertFrom(r -> r.map(e -> OfficeDto.builder()
                                .id(e.value1())
                                .name(e.value2())
                                .address(e.value3())
                                .city(e.value4())
                                .build())),
                        multiset(
                                select(
                                        field("cp.contact_person_id", UUID.class),
                                        field("cp.name", String.class),
                                        field("cp.position", String.class),
                                        multiset(
                                                select(
                                                        field("cd.contact_type", String.class),
                                                        field("cd.value", String.class)
                                                ).from(table("contact_detail").as("cd"))
                                                        .where(field("cd.contact_person_id").eq(field("cp.contact_person_id")))
                                                        .orderBy(field("cd.contact_type", String.class),
                                                                field("cd.value", String.class))
                                        ).convertFrom(r -> r.map(e -> ContactDetailsDto.builder()
                                                .type(ContactType.valueOf(e.value1()))
                                                .value(e.value2())
                                                .build()))
                                ).from(table("contact_person").as("cp"))
                                        .where(field("c.company_id").eq(field("cp.company_id")))
                                        .orderBy(field("cp.name", String.class), field("cp.contact_person_id", UUID.class))
                        ).as("contacts").convertFrom(r -> r.map(e -> ContactPersonDto.builder()
                                .id(e.value1())
                                .name(e.value2())
                                .position(e.value3())
                                .details(e.value4())
                                .build()))
                )
                .from(table("company").as("c"));
    }
}
