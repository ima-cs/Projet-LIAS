package ma.fsbm.lias.config.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

@Entity
@Table(name = "revinfo")
@RevisionEntity
public class RevisionInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @RevisionNumber
    @Column(name = "rev")
    private Integer rev;

    @RevisionTimestamp
    @Column(name = "revtstmp")
    private Long revtstmp;

    public Integer getRev() {
        return rev;
    }

    public Long getRevtstmp() {
        return revtstmp;
    }
}