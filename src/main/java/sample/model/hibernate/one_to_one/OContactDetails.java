package sample.model.hibernate.one_to_one;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import sample.model.ContactType;

@Embeddable
@EqualsAndHashCode
@Setter
@Getter
public class OContactDetails {
    @Column(name = "contact_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private ContactType contactType;

    @Column(name = "value", nullable = false)
    private String value;
}
