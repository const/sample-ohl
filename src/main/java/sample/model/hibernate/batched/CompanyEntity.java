package sample.model.hibernate.batched;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import sample.model.hibernate.EntityUtil;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "company", indexes = @Index(name = "company_by_name_idx", columnList = "name"))
public class CompanyEntity {
    @Id
    @Column(name = "company_id", nullable = false)
    private UUID id;
    @Column(name = "name", nullable = false)
    private String name;
    // skipped
    private String url;
    private String industry;
    private String description;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "company",
            fetch = FetchType.LAZY)
    @BatchSize(size = EntityUtil.BATCH_SIZE)
    private Set<VerificationInfoEntity> verificationInfo;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "company", orphanRemoval = true,
            fetch = FetchType.LAZY)
    @BatchSize(size = EntityUtil.BATCH_SIZE)
    @OrderBy("name, id")
    private Set<ContactPersonEntity> contactPersons = new HashSet<>();
    // skipped
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "company", orphanRemoval = true,
            fetch = FetchType.LAZY)
    @BatchSize(size = EntityUtil.BATCH_SIZE)
    @OrderBy("name, id")
    private Set<OfficeEntity> offices = new HashSet<>();
}
