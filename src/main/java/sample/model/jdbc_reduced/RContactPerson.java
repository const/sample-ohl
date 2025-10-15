package sample.model.jdbc_reduced;

import lombok.*;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.Comparator;
import java.util.UUID;

@Setter
@Getter
@EqualsAndHashCode
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants
@Table(name = "contact_person")
public class RContactPerson {
    public static final Comparator<RContactPerson> COMPARATOR = Comparator.comparing(RContactPerson::getName)
            .thenComparing(RContactPerson::getId);
    @Id
    @Column("contact_person_id")
    private UUID id;
    private String name;
    private String position;
}
