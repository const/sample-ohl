package sample.dto;

import lombok.*;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;
import sample.annotation.OrderBy;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Setter
@Getter
@EqualsAndHashCode
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants
@Table(name = "contact_person")
public class ContactPersonDto {
    public static final Comparator<ContactPersonDto> COMPARATOR = Comparator.comparing(ContactPersonDto::getName)
            .thenComparing(ContactPersonDto::getId);
    @Id
    @Column("contact_person_id")
    private UUID id;
    private String name;
    private String position;
    @MappedCollection(keyColumn = "contact_person_id", idColumn = "contact_person_id")
    @OrderBy({@OrderBy.Value("type"), @OrderBy.Value("id")})
    private List<ContactDetailsDto> details;
}
