package sample.model.hibernate.one_to_one;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import sample.model.hibernate.EntityUtil;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "company", indexes = @Index(name = "company_by_name_idx", columnList = "name"))
public class OCompanyEntity {
    @Id
    @Column(name = "company_id", nullable = false)
    private UUID id;
    @Column(name = "name", nullable = false)
    private String name;
    // skipped
    private String url;
    private String industry;
    private String description;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "company",
            fetch = FetchType.EAGER)
    @Fetch(FetchMode.JOIN)
    @PrimaryKeyJoinColumn
    private OVerificationInfoEntity verificationInfo;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "company", orphanRemoval = true,
            fetch = FetchType.LAZY)
    @BatchSize(size = EntityUtil.BATCH_SIZE)
    @OrderBy("name, id")
    private Set<OContactPersonEntity> contactPersons = new HashSet<>();
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "company", orphanRemoval = true,
            fetch = FetchType.LAZY)
    @BatchSize(size = EntityUtil.BATCH_SIZE)
    @OrderBy("name, id")
    private Set<OOfficeEntity> offices = new HashSet<>();
}
