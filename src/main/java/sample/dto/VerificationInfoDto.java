package sample.dto;

import lombok.*;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import sample.model.VerificationStatus;

import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@FieldNameConstants
@Table("verification_info")
public class VerificationInfoDto {
    private Instant timestamp;
    private VerificationStatus status;
    @Column("username")
    private String user;
    private String comment;
}
