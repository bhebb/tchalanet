package com.tchalanet.server.core.haiti.internal.application.port.out;

import com.tchalanet.server.common.types.id.TchalaEntryId;
import com.tchalanet.server.common.paging.TchPage;
import com.tchalanet.server.core.haiti.domain.tchala.model.DedupeKey;
import com.tchalanet.server.core.haiti.domain.tchala.model.TchalaEntry;
import com.tchalanet.server.core.haiti.domain.tchala.model.TchalaLang;
import com.tchalanet.server.core.haiti.domain.tchala.model.TchalaNumber;
import java.util.List;
import java.util.Optional;

public interface TchalaEntryRepositoryPort {
  Optional<TchalaEntry> findById(TchalaEntryId id);

  Optional<TchalaEntry> findApprovedCanonicalByDedupeKey(TchalaLang lang, DedupeKey key);

  TchPage<TchalaEntry> searchApproved(TchalaLang lang, String text, int page, int size);

  TchPage<TchalaEntry> findApprovedByNumber(
      TchalaLang lang, TchalaNumber number, int page, int size);

  TchPage<TchalaEntry> listPending(TchalaLang lang, boolean conflictOnly, int page, int size);

  TchalaEntry save(TchalaEntry entry);

  // Hard-delete operations (for admin/cleanup). Implementations should remove associated numbers
  // via cascade.
  void deleteById(TchalaEntryId id);

  void deleteAllByIds(List<TchalaEntryId> ids);
}
