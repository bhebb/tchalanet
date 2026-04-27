package com.tchalanet.server.core.haiti.infra.adapter;

import com.tchalanet.server.common.types.id.TchalaEntryId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.core.haiti.application.port.out.TchalaEntryRepositoryPort;
import com.tchalanet.server.core.haiti.domain.tchala.model.*;
import com.tchalanet.server.core.haiti.infra.persistence.entity.TchalaEntryJpaEntity;
import com.tchalanet.server.core.haiti.infra.persistence.repo.TchalaEntrySpringRepository;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class JpaTchalaEntryRepositoryAdapter implements TchalaEntryRepositoryPort {

  private final TchalaEntrySpringRepository repo;

  public JpaTchalaEntryRepositoryAdapter(TchalaEntrySpringRepository repo) {
    this.repo = repo;
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<TchalaEntry> findById(TchalaEntryId id) {
    return repo.findByIdWithNumbers(id.value()).map(TchalaEntryMapper::toDomain);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<TchalaEntry> findApprovedCanonicalByDedupeKey(TchalaLang lang, DedupeKey key) {
    return repo.findCanonicalApprovedByKey(lang.value(), key.key())
        .map(TchalaEntryMapper::toDomain);
  }

  @Override
  @Transactional(readOnly = true)
  public TchPage<TchalaEntry> searchApproved(TchalaLang lang, String text, int page, int size) {
    var p =
        repo.searchApproved(
            lang.value(), text, PageRequest.of(Math.max(0, page), Math.max(1, size)));
    return toTchPage(p);
  }

  @Override
  @Transactional(readOnly = true)
  public TchPage<TchalaEntry> findApprovedByNumber(
      TchalaLang lang, TchalaNumber number, int page, int size) {
    var p =
        repo.findApprovedByNumber(
            lang.value(),
            (short) number.value(),
            PageRequest.of(Math.max(0, page), Math.max(1, size)));
    return toTchPage(p);
  }

  @Override
  @Transactional(readOnly = true)
  public TchPage<TchalaEntry> listPending(
      TchalaLang lang, boolean conflictOnly, int page, int size) {
    var p =
        repo.listPending(
            lang.value(), conflictOnly, PageRequest.of(Math.max(0, page), Math.max(1, size)));
    return toTchPage(p);
  }

  @Override
  public TchalaEntry save(TchalaEntry entry) {
    if (entry == null) throw new IllegalArgumentException("entry required");

    // If exists, load entity (with numbers), update fields, preserve version/createdAt
    if (repo.existsById(entry.id().value())) {
      TchalaEntryJpaEntity existing =
          repo.findByIdWithNumbers(entry.id().value())
              .orElseThrow(() -> new IllegalStateException("entity disappeared"));

      existing.setLang(entry.lang().value());
      existing.setDream(entry.dream().value());
      existing.setDedupeKey(entry.dedupeKey().key());
      existing.setNote(entry.note() == null ? "" : entry.note());
      existing.setStatus(
          com.tchalanet.server.core.haiti.infra.persistence.entity.TchalaEntryStatusDb.valueOf(
              entry.status().name()));
      existing.setSource(
          com.tchalanet.server.core.haiti.infra.persistence.entity.TchalaEntrySourceDb.valueOf(
              entry.source().name()));
      existing.setConflictWithEntryId(
          entry.conflictWithEntryId().map(TchalaEntryId::value).orElse(null));
      existing.setCanonicalEntryId(entry.canonicalEntryId().map(TchalaEntryId::value).orElse(null));
      existing.setUpdatedAt(entry.updatedAt());

      // replace numbers via helper (will clear and recreate)
      existing.replaceNumbers(
          entry.lang().value(), entry.numbers().stream().map(TchalaNumber::value).toList());

      var saved = repo.save(existing);
      return TchalaEntryMapper.toDomain(saved);
    }

    // Insert new entity
    var entity = TchalaEntryMapper.toEntity(entry);
    var saved = repo.save(entity);
    return TchalaEntryMapper.toDomain(saved);
  }

  @Override
  public void deleteById(TchalaEntryId id) {
    repo.deleteById(id.value());
  }

  @Override
  public void deleteAllByIds(List<TchalaEntryId> ids) {
    var uuids = ids.stream().map(TchalaEntryId::value).collect(Collectors.toList());
    // delete in batch to be efficient
    repo.deleteAllByIdInBatch(uuids);
  }

  private TchPage<TchalaEntry> toTchPage(Page<TchalaEntryJpaEntity> page) {
    var items = page.getContent().stream().map(TchalaEntryMapper::toDomain).toList();
    return TchPage.of(
        items,
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages(),
        page.isLast(),
        page.hasNext(),
        page.hasPrevious());
  }
}
