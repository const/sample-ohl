package sample.service.output.layered;


import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import sample.dto.*;
import sample.model.hibernate.batched.CompanyEntity;
import sample.repository.hibernate.CompanyRepository;
import sample.service.output.OutputVariant;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Order(6)
@RequiredArgsConstructor
public class HibernateOutputVariant implements OutputVariant {
    private final CompanyRepository companyRepository;

    public static CompanyDto toDto(CompanyEntity entity) {
        return CompanyDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .industry(entity.getIndustry())
                .description(entity.getDescription())
                .url(entity.getUrl())
                .verified(entity.getVerificationInfo().stream()
                        .map(vi -> VerificationInfoDto.builder()
                                .timestamp(vi.getTimestamp())
                                .user(vi.getUsername())
                                .status(vi.getStatus())
                                .comment(vi.getComment())
                                .build())
                        .findFirst()
                        .orElse(null))
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
        return "Hibernate (Workaround)";
    }

    @Override
    public List<CompanyDto> findList(List<UUID> companyIds) {
        return companyRepository.findByIdInOrderByNameAscIdAsc(companyIds).stream()
                .map(HibernateOutputVariant::toDto)
                .toList();
    }

    @Override
    public Optional<CompanyDto> findSingle(UUID companyId) {
        return companyRepository.findById(companyId)
                .map(HibernateOutputVariant::toDto);
    }
}
