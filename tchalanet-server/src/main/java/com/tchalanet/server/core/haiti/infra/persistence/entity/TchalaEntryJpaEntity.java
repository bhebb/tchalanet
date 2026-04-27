package com.tchalanet.server.core.haiti.infra.persistence.entity;

import com.tchalanet.server.common.persistence.BaseEntity;
import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

@Entity
@Table(name = "tchala_entry")
@Getter
@Setter
@Audited
public class TchalaEntryJpaEntity extends BaseEntity {

  // note: id, createdAt, updatedAt, version, deletedAt, createdBy, updatedBy
  // are inherited from BaseEntity -> AuditableEntity

  @Column(name = "lang", nullable = false, length = 8)
  private String lang;

  @Column(name = "dream", nullable = false, length = 200)
  private String dream;

  @Column(name = "dedupe_key", nullable = false, length = 240)
  private String dedupeKey;

  @Column(name = "note", nullable = false, columnDefinition = "text")
  private String note = "";

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 32)
  private TchalaEntryStatusDb status;

  @Enumerated(EnumType.STRING)
  @Column(name = "source", nullable = false, length = 32)
  private TchalaEntrySourceDb source;

  @Column(name = "conflict_with_entry_id")
  private UUID conflictWithEntryId;

  @Column(name = "canonical_entry_id")
  private UUID canonicalEntryId;

  @OneToMany(
      mappedBy = "entry",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.LAZY)
  private Set<TchalaEntryNumberJpaEntity> numbers = new HashSet<>();

  // Helpers
  public void replaceNumbers(String lang, Iterable<Integer> values) {
    this.numbers.clear();
    for (Integer v : values) {
      var n = new TchalaEntryNumberJpaEntity();
      n.setEntry(this);
      n.setLang(lang);
      n.setNumber(v.shortValue());
      this.numbers.add(n);
    }
  }
}
