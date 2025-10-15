package sample.service.output.layered;


import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import sample.dto.*;
import sample.model.hibernate.one_to_one.OCompanyEntity;
import sample.repository.hibernate.OCompanyRepository;
import sample.service.output.OutputVariant;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Order(5)
@RequiredArgsConstructor
public class HibernateOneToOneOutputVariant implements OutputVariant {
    private final OCompanyRepository companyRepository;

    public static CompanyDto toDto(OCompanyEntity entity) {
        return CompanyDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .industry(entity.getIndustry())
                .description(entity.getDescription())
                .url(entity.getUrl())
                .verified(entity.getVerificationInfo() == null ? null : VerificationInfoDto.builder()
                        .timestamp(entity.getVerificationInfo().getTimestamp())
                        .user(entity.getVerificationInfo().getUsername())
                        .status(entity.getVerificationInfo().getStatus())
                        .comment(entity.getVerificationInfo().getComment())
                        .build())
                .offices(entity.getOffices().stream()
                        .map(o -> OfficeDto.builder()
                                .id(o.getId())
                                .city(o.getCity())
                                .address(o.getAddress())
                                .name(o.getName())
                                .build()
                        )
                        .toList())
                .contactPersons(entity.getContactPersons().stream()
                        .map(p -> ContactPersonDto.builder()
                                .id(p.getId())
                                .name(p.getName())
                                .position(p.getPosition())
                                .details(p.getContactDetails().stream()
                                        .map(d -> ContactDetailsDto.builder()
                                                .type(d.getContactType())
                                                .value(d.getValue())
                                                .build()
                                        ).toList())
                                .build())
                        .toList()).build();
    }

    @Override
    public String getName() {
        return "Hibernate (One To One)";
    }

    @Override
    public List<CompanyDto> findList(List<UUID> companyIds) {
        return companyRepository.findByIdInOrderByNameAscIdAsc(companyIds).stream()
                .map(HibernateOneToOneOutputVariant::toDto)
                .toList();
    }

    @Override
    public Optional<CompanyDto> findSingle(UUID companyId) {
        return companyRepository.findById(companyId)
                .map(HibernateOneToOneOutputVariant::toDto);
    }
}
