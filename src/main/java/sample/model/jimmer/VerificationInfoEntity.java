package sample.model.jimmer;

import jakarta.annotation.Nullable;
import org.babyfish.jimmer.sql.*;
import sample.model.VerificationStatus;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "verification_info_for_jimmer")
public interface VerificationInfoEntity {
    @Id
    @Column(name = "company_id")
    UUID id();
    @OneToOne
    @JoinColumn(name = "company_id_for_jimmer")
    CompanyEntity company();
    @Column(name = "timestamp")
    Instant timestamp();
    @Column(name = "status")
    VerificationStatus status();
    @Column(name = "username")
    String username();
    @Nullable
    String comment();
}
