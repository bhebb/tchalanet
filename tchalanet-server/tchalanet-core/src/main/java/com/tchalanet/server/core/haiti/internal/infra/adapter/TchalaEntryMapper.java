package com.tchalanet.server.core.haiti.internal.infra.adapter;

import com.tchalanet.server.common.types.id.TchalaEntryId;
import com.tchalanet.server.core.haiti.internal.domain.tchala.model.*;
import com.tchalanet.server.core.haiti.internal.infra.persistence.entity.*;
import java.util.Comparator;
import java.util.UUID;

final class TchalaEntryMapper {

  static TchalaEntry toDomain(TchalaEntryJpaEntity entity) {
    var id = TchalaEntryId.of(entity.getId());
    var lang = TchalaLang.of(entity.getLang());
    var dream = DreamText.of(entity.getDream());
    var key = DedupeKey.from(lang, dream);

    var numbers =
        entity.getNumbers().stream()
            .filter(n -> entity.getLang().equals(n.getLang()))
            .map(n -> TchalaNumber.of(n.getPk().getNumber()))
            .sorted(Comparator.comparingInt(TchalaNumber::value))
            .toList();

    return new TchalaEntry(
        id,
        lang,
        dream,
        key,
        numbers,
        entity.getNote(),
        TchalaEntryStatus.valueOf(entity.getStatus().name()),
        TchalaEntrySource.valueOf(entity.getSource().name()),
        java.util.Optional.ofNullable(entity.getConflictWithEntryId()).map(TchalaEntryId::of),
        java.util.Optional.ofNullable(entity.getCanonicalEntryId()).map(TchalaEntryId::of),
        entity.getCreatedAt(),
        entity.getUpdatedAt());
  }

  static TchalaEntryJpaEntity toEntity(TchalaEntry entry) {
    var entity = new TchalaEntryJpaEntity();
    if (entry.id() != null && entry.id().value() != null) {
      entity.setId(entry.id().value());
    }

    entity.setLang(entry.lang().value());
    entity.setDream(entry.dream().value());
    entity.setDedupeKey(entry.dedupeKey().key());
    entity.setNote(entry.note() == null ? "" : entry.note());
    entity.setStatus(TchalaEntryStatusDb.valueOf(entry.status().name()));
    entity.setSource(TchalaEntrySourceDb.valueOf(entry.source().name()));
    entity.setConflictWithEntryId(entry.conflictWithEntryId().map(TchalaEntryId::value).orElse(null));
    entity.setCanonicalEntryId(entry.canonicalEntryId().map(TchalaEntryId::value).orElse(null));

    entity.replaceNumbers(entry.lang().value(), entry.numbers().stream().map(TchalaNumber::value).toList());

    return entity;
  }

  static UUID id(TchalaEntryId id) {
    return id.value();
  }
}
