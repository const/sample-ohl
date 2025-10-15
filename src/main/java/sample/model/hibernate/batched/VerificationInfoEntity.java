package sample.model.hibernate.batched;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import sample.model.VerificationStatus;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "verification_info", indexes = @Index(name = "verification_info_by_company_id_idx", columnList = "company_id"))
@Getter
@Setter
public class VerificationInfoEntity {
    @Id
    @Column(name = "company_id", nullable = false)
    private UUID companyId;
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "company_id", nullable = false)
    private CompanyEntity company;
    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private VerificationStatus status;
    @Column(name = "username", nullable = false)
    private String username;
    private String comment;
}
