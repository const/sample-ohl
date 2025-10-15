package sample.service.output;

import sample.dto.CompanyDto;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OutputVariant {
    String getName();

    default boolean isSingleSupported() {
        return true;
    }

    default boolean isListSupported() {
        return true;
    }

    List<CompanyDto> findList(List<UUID> companyIds);

    Optional<CompanyDto> findSingle(UUID companyId);
}
