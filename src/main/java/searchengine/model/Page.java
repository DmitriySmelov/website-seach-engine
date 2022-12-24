package searchengine.model;

import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.util.Set;

@Entity
@NoArgsConstructor
@Data
@Getter
@Setter
@Table(name = "pages")
public class Page
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false)
    private String path;

    private int code;

    @Column(nullable = false)
    private String content;

    @ManyToOne()
    @JoinColumn(name = "site_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Site site;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "indexes",
            joinColumns = @JoinColumn(name = "page_id"),
            inverseJoinColumns = @JoinColumn(name = "lemma_id"))
    private Set<Lemma> lemmas;

    public void addLemma(Lemma lemma)
    {
        lemmas.add(lemma);
        lemma.getPages().add(this);
    }

    public void removePage(Lemma lemma)
    {
        lemmas.remove(lemma);
        lemma.getPages().remove(this);
    }
}
