package sample.repository.jdbc;

import org.springframework.data.repository.CrudRepository;
import sample.model.jdbc_reduced.RCompany;

import java.util.List;
import java.util.UUID;

public interface RCompanyRepositoryJdbc extends CrudRepository<RCompany, UUID> {
    List<RCompany> findByIdInOrderByNameAscIdAsc(List<UUID> idList);
}
