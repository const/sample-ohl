package sample.dto;

import lombok.*;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;
import sample.annotation.OrderBy;

import java.util.List;
import java.util.UUID;

@Setter
@Getter
@EqualsAndHashCode
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants
@Table(name = "company")
public class CompanyDto {
    @Id
    @Column("company_id")
    private UUID id;
    private String name;
    private String industry;
    private String description;
    private String url;
    @MappedCollection(idColumn = "company_id", keyColumn = "company_id")
    private VerificationInfoDto verified;
    @MappedCollection(idColumn = "company_id", keyColumn = "company_id")
    @OrderBy({@OrderBy.Value("name"), @OrderBy.Value("id")})
    private List<ContactPersonDto> contactPersons;
    @MappedCollection(idColumn = "company_id", keyColumn = "company_id")
    @OrderBy({@OrderBy.Value("name"), @OrderBy.Value("id")})
    private List<OfficeDto> offices;
}
