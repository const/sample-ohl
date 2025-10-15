package sample.service.output.layered;


import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import sample.dto.*;
import sample.model.jimmer.CompanyEntity;
import sample.model.jimmer.CompanyEntityFetcher;
import sample.model.jimmer.Fetchers;
import sample.repository.jimmer.CompanyJimmerRepository;
import sample.service.output.OutputVariant;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Order(4)
@RequiredArgsConstructor
public class JimmerOutputVariant implements OutputVariant {
    public static final CompanyEntityFetcher COMPANY_FETCH_ALL = Fetchers.COMPANY_ENTITY_FETCHER
            .allScalarFields()
            .verificationInfo(Fetchers.VERIFICATION_INFO_ENTITY_FETCHER.allScalarFields())
            .offices(Fetchers.OFFICE_ENTITY_FETCHER.allScalarFields())
            .contacts(Fetchers.CONTACT_PERSON_ENTITY_FETCHER
                    .details(Fetchers.CONTACT_DETAIL_ENTITY_FETCHER.allScalarFields())
                    .allScalarFields());
    private final CompanyJimmerRepository companyRepository;

    public static CompanyDto toDto(CompanyEntity entity) {
        return CompanyDto.builder()
                .id(entity.id())
                .name(entity.name())
                .industry(entity.industry())
                .description(entity.description())
                .url(entity.url())
                .verified(Optional.ofNullable(entity.verificationInfo())
                        .map(vi -> VerificationInfoDto.builder()
                                .timestamp(vi.timestamp())
                                .user(vi.username())
                                .status(vi.status())
                                .comment(vi.comment())
                                .build())
                        .orElse(null))
                .offices(entity.offices().stream()
                        .map(o -> OfficeDto.builder()
                                .id(o.id())
                                .city(o.city())
                                .address(o.address())
                                .name(o.name())
                                .build()
                        )
                        .toList())
                .contactPersons(entity.contacts().stream()
                        .map(p -> ContactPersonDto.builder()
                                .id(p.id())
                                .name(p.name())
                                .position(p.position())
                                .details(p.details().stream()
                                        .map(d -> ContactDetailsDto.builder()
                                                .type(d.type())
                                                .value(d.value())
                                                .build()
                                        ).toList())
                                .build())
                        .toList()).build();
    }

    @Override
    public String getName() {
        return "Jimmer ORM";
    }

    @Override
    public List<CompanyDto> findList(List<UUID> companyIds) {
        return companyRepository.findByIdInOrderByNameAscIdAsc(companyIds, COMPANY_FETCH_ALL).stream()
                .map(JimmerOutputVariant::toDto)
                .toList();
    }

    @Override
    public Optional<CompanyDto> findSingle(UUID companyId) {
        return companyRepository.findById(companyId, COMPANY_FETCH_ALL)
                .map(JimmerOutputVariant::toDto);
    }
}
