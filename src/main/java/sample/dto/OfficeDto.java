package sample.dto;

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
@Table("company_office")
public class OfficeDto {
    public static final Comparator<OfficeDto> COMPARATOR = Comparator.comparing(OfficeDto::getName)
            .thenComparing(OfficeDto::getId);
    @Id
    @Column("office_id")
    private UUID id;
    private String name;
    private String city;
    private String address;
}
