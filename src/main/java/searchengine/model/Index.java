package searchengine.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.*;

import javax.persistence.*;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "indexes")
@NoArgsConstructor
@Getter
@Setter
@DynamicInsert
@DynamicUpdate
public class Index {

    @Id
    @GeneratedValue(generator = "index_generator")
    @GenericGenerator(name = "index_generator", strategy = "increment")
    private int id;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
    @JoinColumn(name = "page_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Page page;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
    @JoinColumn(name = "lemma_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Lemma lemma;

    private float grade;
}
