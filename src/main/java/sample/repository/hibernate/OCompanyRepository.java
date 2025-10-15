package sample.repository.hibernate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import sample.model.hibernate.one_to_one.OCompanyEntity;

import java.util.List;
import java.util.UUID;

public interface OCompanyRepository extends JpaRepository<OCompanyEntity, UUID>, JpaSpecificationExecutor<OCompanyEntity> {
    List<OCompanyEntity> findByIdInOrderByNameAscIdAsc(List<UUID> idList);
}
