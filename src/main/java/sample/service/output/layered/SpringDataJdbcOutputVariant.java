package sample.service.output.layered;

import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sample.dto.*;
import sample.repository.jdbc.CompanyRepositoryJdbc;
import sample.service.output.OutputVariant;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Order(8)
public class SpringDataJdbcOutputVariant implements OutputVariant {
    private final CompanyRepositoryJdbc companyRepositoryJdbc;

    @Override
    public String getName() {
        return "Spring Data JDBC";
    }

    @Override
    @Transactional
    public List<CompanyDto> findList(List<UUID> companyIds) {
        return companyRepositoryJdbc.findByIdInOrderByNameAscIdAsc(companyIds).stream()
                .map(this::toDto)
                .toList();
    }


    @Override
    public Optional<CompanyDto> findSingle(UUID companyId) {
        return companyRepositoryJdbc.findById(companyId)
                .map(this::toDto);
    }

    private CompanyDto toDto(CompanyDto companyDto) {
        companyDto.getOffices().sort(OfficeDto.COMPARATOR);
        companyDto.getContactPersons().sort(ContactPersonDto.COMPARATOR);
        for (ContactPersonDto contactPerson : companyDto.getContactPersons()) {
            contactPerson.getDetails().sort(ContactDetailsDto.COMPARATOR);
        }
        return companyDto;
    }
}
