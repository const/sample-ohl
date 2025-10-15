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
@Order(24)
public class JooqReflectionOutputVariant implements OutputVariant {

    private final DSLContext create;

    @Override
    public String getName() {
        return "jOOQ (Reflection)";
    }

    @Override
    public List<CompanyDto> findList(List<UUID> companyIds) {
        var select = getBaseSelect()
                .where(field("c.company_id", UUID.class).eq(any(companyIds.toArray(UUID[]::new))))
                .orderBy(field("c.name", String.class), field("c.company_id", UUID.class));
        return select.fetch(r -> r.into(CompanyDto.class));
    }

    @Override
    public Optional<CompanyDto> findSingle(UUID companyId) {
        var select = getBaseSelect()
                .where(field("c.company_id", UUID.class).eq(companyId));
        return select.fetchOptional(r -> r.into(CompanyDto.class));
    }

    private SelectJoinStep<Record8<UUID, String, String, String, String, VerificationInfoDto, List<OfficeDto>, List<ContactPersonDto>>> getBaseSelect() {
        return create.select(
                        field("c.company_id", UUID.class).as(CompanyDto.Fields.id),
                        field("c.name", String.class).as(CompanyDto.Fields.name),
                        field("c.industry", String.class).as(CompanyDto.Fields.industry),
                        field("c.description", String.class).as(CompanyDto.Fields.description),
                        field("c.url", String.class).as(CompanyDto.Fields.url),
                        field(
                                select(
                                        row(
                                                field("vi.username", String.class),
                                                field("vi.timestamp", OffsetDateTime.class)
                                                        .convertFrom(OffsetDateTime::toInstant),
                                                field("vi.status", String.class)
                                                        .convertFrom(VerificationStatus::valueOf),
                                                field("vi.comment", String.class)
                                        )
                                ).from(table("verification_info").as("vi"))
                                        .where(field("c.company_id").eq(field("vi.company_id")))
                        ).convertFrom(r -> r == null
                                ? null : VerificationInfoDto.builder().user(r.value1()).timestamp(r.value2())
                                .status(r.value3()).comment(r.value4()).build()
                        ).as(CompanyDto.Fields.verified),
                        multiset(
                                select(
                                        field("o.office_id", UUID.class).as(OfficeDto.Fields.id),
                                        field("o.name", String.class).as(OfficeDto.Fields.name),
                                        field("o.address", String.class).as(OfficeDto.Fields.address),
                                        field("o.city", String.class).as(OfficeDto.Fields.city)
                                ).from(table("company_office").as("o"))
                                        .where(field("c.company_id").eq(field("o.company_id")))
                                        .orderBy(field("o.name", String.class),
                                                field("o.office_id", UUID.class))
                        ).as(CompanyDto.Fields.offices).convertFrom(r -> r.into(OfficeDto.class)),
                        multiset(
                                select(
                                        field("cp.contact_person_id", UUID.class).as(ContactPersonDto.Fields.id),
                                        field("cp.name", String.class).as("name").as(ContactPersonDto.Fields.name),
                                        field("cp.position", String.class).as("position").as(ContactPersonDto.Fields.position),
                                        multiset(
                                                select(
                                                        field("cd.contact_type", String.class)
                                                                .convertFrom(ContactType::valueOf)
                                                                .as(ContactDetailsDto.Fields.type), // because column annotation is attached there
                                                        field("cd.value", String.class).as(ContactDetailsDto.Fields.value)
                                                ).from(table("contact_detail").as("cd"))
                                                        .where(field("cd.contact_person_id").eq(field("cp.contact_person_id")))
                                                        .orderBy(field("cd.contact_type", String.class), field("cd.value", String.class))
                                        ).convertFrom(r -> r.into(ContactDetailsDto.class)).as(ContactPersonDto.Fields.details)
                                ).from(table("contact_person").as("cp"))
                                        .where(field("c.company_id").eq(field("cp.company_id")))
                                        .orderBy(field("cp.name", String.class),
                                                field("cp.contact_person_id", UUID.class))
                        ).as(CompanyDto.Fields.contactPersons).convertFrom(r -> r.into(ContactPersonDto.class))
                )
                .from(table("company").as("c"));
    }
}
