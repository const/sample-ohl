package sample.model.jimmer;

import jakarta.annotation.Nullable;
import org.babyfish.jimmer.sql.*;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "company", schema = "public")
public interface CompanyEntity {
    @Id
    @Column(name = "company_id")
    UUID id();
    @Column(name = "name")
    String name();
    // skipped
    @Nullable
    String url();
    @Nullable
    String industry();
    @Nullable
    String description();

    @OneToOne(mappedBy = "company", targetTransferMode = TargetTransferMode.ALLOWED)
    @Nullable
    VerificationInfoEntity verificationInfo();

    @OneToMany(mappedBy = "company", orderedProps = {@OrderedProp("name"), @OrderedProp("id")})
    List<ContactPersonEntity> contacts();

    @OneToMany(mappedBy = "company", orderedProps = {@OrderedProp("name"), @OrderedProp("id")})
    List<OfficeEntity> offices();
}