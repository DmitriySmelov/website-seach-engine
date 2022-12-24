package searchengine.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.*;

import javax.persistence.*;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.Set;

@Entity
@Table(name = "lemmas")
@NoArgsConstructor
@Getter
@Setter
public class Lemma
{
    @Id
    @GeneratedValue(generator = "lemma_generator")
    @GenericGenerator(name = "lemma_generator", strategy = "increment")
    private int id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Site site;

    @Column(nullable = false)
    private String lemma;

    private int frequency;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "indexes",
            joinColumns = @JoinColumn(name = "lemma_id"),
            inverseJoinColumns = @JoinColumn(name = "page_id"))
    private Set<Page> pages;


    public void addPage(Page page)
    {
        pages.add(page);
        page.getLemmas().add(this);
    }

    public void removePage(Page page)
    {
        pages.remove(page);
        page.getLemmas().remove(this);
    }
}
