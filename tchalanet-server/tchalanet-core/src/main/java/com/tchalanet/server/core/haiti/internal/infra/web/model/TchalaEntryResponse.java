package com.tchalanet.server.core.haiti.internal.infra.web.model;

import com.tchalanet.server.core.haiti.domain.tchala.model.TchalaEntry;
import java.util.List;
import java.util.UUID;

/**
 * DTO returned by public and admin endpoints for a Tchala entry. Includes optional fields used by
 * admin UI.
 */
public record TchalaEntryResponse(
    UUID id,
    String lang,
    String dream,
    List<Integer> numbers,
    String note,
    String status,
    String source,
    UUID conflictWithEntryId,
    UUID canonicalEntryId) {

  public static TchalaEntryResponse from(TchalaEntry e) {
    var nums = e.numbers().stream().map(n -> n.value()).toList();
    return new TchalaEntryResponse(
        e.id().value(),
        e.lang().value(),
        e.dream().value(),
        nums,
        e.note(),
        e.status().name(),
        e.source().name(),
        e.conflictWithEntryId().map(x -> x.value()).orElse(null),
        e.canonicalEntryId().map(x -> x.value()).orElse(null));
  }
}
