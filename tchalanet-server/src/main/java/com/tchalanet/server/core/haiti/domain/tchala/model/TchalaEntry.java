package com.tchalanet.server.core.haiti.domain.tchala.model;

import com.tchalanet.server.common.types.id.TchalaEntryId;
import com.tchalanet.server.core.haiti.domain.tchala.exception.InvalidTchalaEntryException;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public record TchalaEntry(
    TchalaEntryId id,
    TchalaLang lang,
    DreamText dream,
    DedupeKey dedupeKey,
    List<TchalaNumber> numbers,
    String note,
    TchalaEntryStatus status,
    TchalaEntrySource source,
    Optional<TchalaEntryId> conflictWithEntryId,
    Optional<TchalaEntryId> canonicalEntryId,
    Instant createdAt,
    Instant updatedAt) {

  public static TchalaEntry newSuggestion(
      TchalaEntryId id,
      TchalaLang lang,
      DreamText dream,
      List<TchalaNumber> numbers,
      String note,
      Optional<TchalaEntryId> conflictWith,
      Instant now) {
    Objects.requireNonNull(id);
    Objects.requireNonNull(lang);
    Objects.requireNonNull(dream);
    Objects.requireNonNull(numbers);
    Objects.requireNonNull(now);
    if (numbers.isEmpty()) throw new InvalidTchalaEntryException("numbers empty");
    List<TchalaNumber> nums =
        numbers.stream()
            .distinct()
            .sorted((a, b) -> Integer.compare(a.value(), b.value()))
            .collect(Collectors.toUnmodifiableList());
    String n = note == null ? "" : note.trim();
    DedupeKey key = DedupeKey.from(lang, dream);
    return new TchalaEntry(
        id,
        lang,
        dream,
        key,
        nums,
        n,
        TchalaEntryStatus.PENDING,
        TchalaEntrySource.PUBLIC_SUGGESTION,
        conflictWith,
        Optional.empty(),
        now,
        now);
  }

  // Factory for import-based suggestions (source = IMPORT)
  public static TchalaEntry newSuggestionFromImport(
      TchalaEntryId id,
      TchalaLang lang,
      DreamText dream,
      List<TchalaNumber> numbers,
      String note,
      Optional<TchalaEntryId> conflictWith,
      Instant now) {
    Objects.requireNonNull(id);
    Objects.requireNonNull(lang);
    Objects.requireNonNull(dream);
    Objects.requireNonNull(numbers);
    Objects.requireNonNull(now);
    if (numbers.isEmpty()) throw new InvalidTchalaEntryException("numbers empty");
    List<TchalaNumber> nums =
        numbers.stream()
            .distinct()
            .sorted((a, b) -> Integer.compare(a.value(), b.value()))
            .collect(Collectors.toUnmodifiableList());
    String n = note == null ? "" : note.trim();
    DedupeKey key = DedupeKey.from(lang, dream);
    return new TchalaEntry(
        id,
        lang,
        dream,
        key,
        nums,
        n,
        TchalaEntryStatus.PENDING,
        TchalaEntrySource.IMPORT,
        conflictWith,
        Optional.empty(),
        now,
        now);
  }

  public static TchalaEntry newCanonical(
      TchalaEntryId id,
      TchalaLang lang,
      DreamText dream,
      List<TchalaNumber> numbers,
      String note,
      TchalaEntrySource source,
      Instant now) {
    Objects.requireNonNull(id);
    Objects.requireNonNull(lang);
    Objects.requireNonNull(dream);
    Objects.requireNonNull(numbers);
    Objects.requireNonNull(source);
    Objects.requireNonNull(now);
    if (numbers.isEmpty()) throw new InvalidTchalaEntryException("numbers empty");
    List<TchalaNumber> nums =
        numbers.stream()
            .distinct()
            .sorted((a, b) -> Integer.compare(a.value(), b.value()))
            .collect(Collectors.toUnmodifiableList());
    String n = note == null ? "" : note.trim();
    DedupeKey key = DedupeKey.from(lang, dream);
    return new TchalaEntry(
        id,
        lang,
        dream,
        key,
        nums,
        n,
        TchalaEntryStatus.APPROVED,
        source,
        Optional.empty(),
        Optional.empty(),
        now,
        now);
  }

  public TchalaEntry approveAsCanonical(Instant now) {
    if (status != TchalaEntryStatus.PENDING)
      throw new InvalidTchalaEntryException("only pending can be approved");
    return new TchalaEntry(
        id,
        lang,
        dream,
        dedupeKey,
        numbers,
        note,
        TchalaEntryStatus.APPROVED,
        source,
        conflictWithEntryId,
        Optional.empty(),
        createdAt,
        now);
  }

  public TchalaEntry reject(String reason, Instant now) {
    String newNote = (note == null ? "" : note) + "\nREJECT_REASON: " + reason;
    return new TchalaEntry(
        id,
        lang,
        dream,
        dedupeKey,
        numbers,
        newNote,
        TchalaEntryStatus.REJECTED,
        source,
        conflictWithEntryId,
        canonicalEntryId,
        createdAt,
        now);
  }

  public TchalaEntry markMergedInto(TchalaEntryId canonicalId, Instant now) {
    return new TchalaEntry(
        id,
        lang,
        dream,
        dedupeKey,
        numbers,
        note,
        TchalaEntryStatus.MERGED,
        source,
        Optional.ofNullable(canonicalId),
        Optional.ofNullable(canonicalId),
        createdAt,
        now);
  }

  public TchalaEntry archive(Instant now) {
    return new TchalaEntry(
        id,
        lang,
        dream,
        dedupeKey,
        numbers,
        note,
        TchalaEntryStatus.ARCHIVED,
        source,
        conflictWithEntryId,
        canonicalEntryId,
        createdAt,
        now);
  }
}
