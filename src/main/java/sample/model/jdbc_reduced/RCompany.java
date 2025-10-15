package sample.model.jdbc_reduced;

import lombok.*;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

import java.util.Set;
import java.util.UUID;

@Setter
@Getter
@EqualsAndHashCode
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants
@Table(name = "company")
public class RCompany {
    @Id
    @Column("company_id")
    private UUID id;
    private String name;
    private String industry;
    private String description;
    private String url;
    @MappedCollection(idColumn = "company_id", keyColumn = "company_id")
    private Set<RContactPerson> contactPersons;
    @MappedCollection(idColumn = "company_id", keyColumn = "company_id")
    private Set<ROffice> offices;
}
