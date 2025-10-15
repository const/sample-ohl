package sample.model.jimmer;

import org.babyfish.jimmer.sql.*;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "contact_person")
public interface ContactPersonEntity {
    @Id
    @Column(name = "contact_person_id")
    UUID id();

    @ManyToOne
    @JoinColumn(name = "company_id")
    CompanyEntity company();

    @Column(name = "name")
    String name();

    @Column(name = "position")
    String position();

    @OneToMany(mappedBy = "contactPerson", orderedProps = {@OrderedProp("type"), @OrderedProp("value")})
    List<ContactDetailEntity> details();
}
