package sample.model.hibernate.one_to_one;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Setter
@Getter
@Entity
@Table(name = "company_office", indexes = @Index(name = "company_office_by_company_id_idx", columnList = "company_id"))
public class OOfficeEntity {
    @Id
    @Column(name = "office_id")
    private UUID id;
    @JoinColumn(name = "company_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private OCompanyEntity company;
    @Column(name = "name", nullable = false)
    private String name;
    @Column(name = "city", nullable = false)
    private String city;
    @Column(name = "address", nullable = false)
    private String address;
}
