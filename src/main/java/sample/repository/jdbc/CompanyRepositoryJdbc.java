package sample.repository.jdbc;

import org.springframework.data.repository.CrudRepository;
import sample.dto.CompanyDto;

import java.util.List;
import java.util.UUID;

public interface CompanyRepositoryJdbc extends CrudRepository<CompanyDto, UUID> {
    List<CompanyDto> findByIdInOrderByNameAscIdAsc(List<UUID> idList);
}
