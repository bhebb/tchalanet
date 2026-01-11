package com.tchalanet.server.core.haiti.infra.persistence.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tchala_entry_number")
@Getter
@Setter
public class TchalaEntryNumberJpaEntity {

  @EmbeddedId private Pk pk = new Pk();

  @MapsId("entryId")
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "entry_id", nullable = false)
  private TchalaEntryJpaEntity entry;

  @Column(name = "lang", nullable = false, length = 8)
  private String lang;

  @Column(name = "number", nullable = false)
  private short number;

  public void setNumber(short number) {
    this.number = number;
    this.pk.number = number;
  }

  public void setEntry(TchalaEntryJpaEntity entry) {
    this.entry = entry;
    this.pk.entryId = entry.getId();
  }

  @Embeddable
  @Getter
  @Setter
  public static class Pk implements Serializable {
    @Column(name = "entry_id", nullable = false)
    private java.util.UUID entryId;

    @Column(name = "number", nullable = false)
    private short number;

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof Pk pk)) return false;
      return number == pk.number && Objects.equals(entryId, pk.entryId);
    }

    @Override
    public int hashCode() {
      return Objects.hash(entryId, number);
    }
  }
}
