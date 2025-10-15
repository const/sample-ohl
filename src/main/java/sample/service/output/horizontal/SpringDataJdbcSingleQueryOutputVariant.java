package sample.service.output.horizontal;

import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sample.dto.CompanyDto;
import sample.dto.ContactPersonDto;
import sample.dto.OfficeDto;
import sample.model.jdbc_reduced.RCompany;
import sample.repository.jdbc.RCompanyRepositoryJdbc;
import sample.service.output.OutputVariant;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
@Order(35)
public class SpringDataJdbcSingleQueryOutputVariant implements OutputVariant {
    private final RCompanyRepositoryJdbc companyRepositoryJdbc;

    @Override
    public String getName() {
        return "Spring Data JDBC (Single Query)";
    }

    @Override
    @Transactional
    public List<CompanyDto> findList(List<UUID> companyIds) {
        return StreamSupport.stream(companyRepositoryJdbc.findAllById(companyIds).spliterator(), false)
                .map(this::toDto)
                .toList();
    }

    @Override
    public Optional<CompanyDto> findSingle(UUID companyId) {
        return companyRepositoryJdbc.findById(companyId)
                .map(this::toDto);
    }

    private CompanyDto toDto(RCompany entity) {
        return CompanyDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .industry(entity.getIndustry())
                .description(entity.getDescription())
                .url(entity.getUrl())
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
                                .build())
                        .toList())
                .build();
    }
}
