package sample.dto;

import lombok.*;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import sample.model.ContactType;

import java.util.Comparator;


@Setter
@Getter
@EqualsAndHashCode
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants
@Table("contact_detail")
public class ContactDetailsDto {
    public static final Comparator<ContactDetailsDto> COMPARATOR = Comparator.<ContactDetailsDto, String>comparing(d -> d.getType().name())
            .thenComparing(ContactDetailsDto::getValue);
    @Column("contact_type")
    private ContactType type;
    @Column("value")
    private String value;

    public ContactType getType() {
        return type;
    }

    public void setType(ContactType type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
