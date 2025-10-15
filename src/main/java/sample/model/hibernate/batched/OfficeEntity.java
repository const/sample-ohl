package sample.model.hibernate.batched;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Setter
@Getter
@Entity
@Table(name = "company_office", indexes = @Index(name = "company_office_by_company_id_idx", columnList = "company_id"))
public class OfficeEntity {
    @Id
    @Column(name = "office_id")
    private UUID id;
    @JoinColumn(name = "company_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private CompanyEntity company;
    @Column(name = "name", nullable = false)
    private String name;
    @Column(name = "city", nullable = false)
    private String city;
    @Column(name = "address", nullable = false)
    private String address;
}
