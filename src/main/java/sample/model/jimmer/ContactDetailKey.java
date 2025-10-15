package sample.model.jimmer;


import org.babyfish.jimmer.sql.Column;
import org.babyfish.jimmer.sql.Embeddable;
import sample.model.ContactType;

@Embeddable
public interface ContactDetailKey {
    @Column(name = "contact_person_id")
    String contactPersonId();

    @Column(name = "contact_type")
    ContactType type();

    String value();
}
