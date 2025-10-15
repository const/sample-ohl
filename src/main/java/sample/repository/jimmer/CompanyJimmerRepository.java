package sample.repository.jimmer;

import jakarta.annotation.Nullable;
import org.babyfish.jimmer.spring.repository.JRepository;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import sample.model.jimmer.CompanyEntity;

import java.util.List;
import java.util.UUID;

public interface CompanyJimmerRepository extends JRepository<CompanyEntity, UUID> {
    List<CompanyEntity> findByIdInOrderByNameAscIdAsc(List<UUID> idList, @Nullable Fetcher<CompanyEntity> fetcher);
}
