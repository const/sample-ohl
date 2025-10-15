package sample.model.hibernate.one_to_one;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import sample.model.hibernate.EntityUtil;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "contact_person", indexes = @Index(name = "contact_persons_by_company_id_idx", columnList = "company_id"))
public class OContactPersonEntity {
    @Id
    @Column(name = "contact_person_id", nullable = false)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private OCompanyEntity company;
    @Column(name = "name", nullable = false)
    private String name;
    @Column(name = "position")
    private String position;
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "contact_detail", joinColumns = @JoinColumn(name = "contact_person_id"),
            indexes = {
                    @Index(name = "contact_detail_by_contact_person_id_idx", columnList = "contact_person_id"),
                    @Index(name = "contact_detail_by_value_contact_person_id_idx", columnList = "value, contact_person_id")
            })
    @BatchSize(size = EntityUtil.BATCH_SIZE * 3)
    @OrderBy("contactType,value")
    private Set<OContactDetails> contactDetails = new HashSet<>();
}
