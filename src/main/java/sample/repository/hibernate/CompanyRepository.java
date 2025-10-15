package sample.repository.hibernate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import sample.model.hibernate.batched.CompanyEntity;

import java.util.List;
import java.util.UUID;

public interface CompanyRepository extends JpaRepository<CompanyEntity, UUID>, JpaSpecificationExecutor<CompanyEntity> {
    List<CompanyEntity> findByIdInOrderByNameAscIdAsc(List<UUID> idList);
}
