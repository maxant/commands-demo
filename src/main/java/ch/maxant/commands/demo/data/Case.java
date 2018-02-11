package ch.maxant.commands.demo.data;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "T_CASE")
@NamedQueries({
        @NamedQuery(name = Case.NQFindAll.NAME, query = Case.NQFindAll.QUERY),
        @NamedQuery(name = Case.NQFindByNumber.NAME, query = Case.NQFindByNumber.QUERY)
})
public class Case {

    public static class NQFindAll {
        public static final String NAME = "Case.findAll";
        public static final String QUERY = "select c from Case c";
    }

    public static class NQFindByNumber {
        public static final String NAME = "Case.findByNumber";
        public static final String PARAM_NR = "nr";
        public static final String QUERY = "select c from Case c where c.nr = :" + PARAM_NR;
    }

    @Id
    @Column(name = "ID")
    private String id = UUID.randomUUID().toString();

    @Column(name = "NR", nullable = false)
    private long nr;

    @Column(name = "DESCRIPTION", length = 100)
    private String description;

    @Column(name = "CREATED")
    private LocalDateTime created;

    @PrePersist
    public void prePersist(){
        this.created = LocalDateTime.now();
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public long getNr() {
        return nr;
    }

    public String getDescription() {
        return description;
    }

    public String getId() {
        return id;
    }

    public void setDescription(String description) {
        this.description = description;
    }

}
