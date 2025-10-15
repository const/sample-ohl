package sample.model.jimmer;

import org.babyfish.jimmer.sql.*;

import java.util.UUID;

@Entity
@Table(name = "company_office")
public interface OfficeEntity {
    @Id
    @Column(name = "office_id")
    UUID id();

    @ManyToOne
    @JoinColumn(name = "company_id")
    CompanyEntity company();

    @Column(name = "name")
    String name();

    @Column(name = "city")
    String city();

    @Column(name = "address")
    String address();
}
