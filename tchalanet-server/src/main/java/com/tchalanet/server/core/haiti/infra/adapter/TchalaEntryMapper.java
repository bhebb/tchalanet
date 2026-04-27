package com.tchalanet.server.core.haiti.infra.adapter;

import com.tchalanet.server.common.types.id.TchalaEntryId;
import com.tchalanet.server.core.haiti.domain.tchala.model.*;
import com.tchalanet.server.core.haiti.infra.persistence.entity.*;
import java.util.Comparator;
import java.util.UUID;

final class TchalaEntryMapper {

  static TchalaEntry toDomain(TchalaEntryJpaEntity e) {
    var id = TchalaEntryId.of(e.getId());
    var lang = TchalaLang.of(e.getLang());
    var dream = DreamText.of(e.getDream());
    var key = DedupeKey.from(lang, dream);

    var numbers =
        e.getNumbers().stream()
            .filter(n -> e.getLang().equals(n.getLang()))
            .map(n -> TchalaNumber.of(n.getNumber()))
            .sorted(Comparator.comparingInt(TchalaNumber::value))
            .toList();

    return new TchalaEntry(
        id,
        lang,
        dream,
        key,
        numbers,
        e.getNote(),
        TchalaEntryStatus.valueOf(e.getStatus().name()),
        TchalaEntrySource.valueOf(e.getSource().name()),
        java.util.Optional.ofNullable(e.getConflictWithEntryId()).map(TchalaEntryId::of),
        java.util.Optional.ofNullable(e.getCanonicalEntryId()).map(TchalaEntryId::of),
        e.getCreatedAt(),
        e.getUpdatedAt());
  }

  static TchalaEntryJpaEntity toEntity(TchalaEntry d) {
    var e = new TchalaEntryJpaEntity();
    // Do not explicitly set id or audit fields here. BaseEntity/AuditableEntity
    // will manage id (prePersist) and createdAt/updatedAt/version via JPA auditing.
    if (d.id() != null && d.id().value() != null) {
      // If domain provides an id and we want to preserve it, set it. Otherwise leave null for DB
      // generation.
      e.setId(d.id().value());
    }

    e.setLang(d.lang().value());
    e.setDream(d.dream().value());
    e.setDedupeKey(d.dedupeKey().key());
    e.setNote(d.note() == null ? "" : d.note());
    e.setStatus(TchalaEntryStatusDb.valueOf(d.status().name()));
    e.setSource(TchalaEntrySourceDb.valueOf(d.source().name()));
    e.setConflictWithEntryId(d.conflictWithEntryId().map(TchalaEntryId::value).orElse(null));
    e.setCanonicalEntryId(d.canonicalEntryId().map(TchalaEntryId::value).orElse(null));

    // replaceNumbers will populate the tchala_entry_number rows
    e.replaceNumbers(d.lang().value(), d.numbers().stream().map(TchalaNumber::value).toList());

    return e;
  }

  static UUID id(TchalaEntryId id) {
    return id.value();
  }
}
