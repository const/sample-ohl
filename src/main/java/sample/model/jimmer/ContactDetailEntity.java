package sample.model.jimmer;


import org.babyfish.jimmer.sql.*;
import sample.model.ContactType;

@Entity
@Table(name = "contact_detail_for_jimmer")
public interface ContactDetailEntity {
    @Id
    ContactDetailKey id();
    @ManyToOne
    @JoinColumn(name = "contact_person_id_for_jimmer")
    ContactPersonEntity contactPerson();
    @Column(name = "contact_type_for_jimmer")
    ContactType type();
    @Column(name = "value_for_jimmer")
    String value();
}
