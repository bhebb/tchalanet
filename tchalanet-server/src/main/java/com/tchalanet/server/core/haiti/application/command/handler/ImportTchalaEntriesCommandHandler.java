package com.tchalanet.server.core.haiti.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.common.types.id.TchalaEntryId;
import com.tchalanet.server.core.haiti.application.command.model.ImportTchalaEntriesCommand;
import com.tchalanet.server.core.haiti.application.command.model.ImportTchalaEntriesCommand.ImportRow;
import com.tchalanet.server.core.haiti.application.port.out.TchalaEntryRepositoryPort;
import com.tchalanet.server.core.haiti.application.port.out.TchalaImportSourcePort;
import com.tchalanet.server.core.haiti.domain.tchala.model.DreamText;
import com.tchalanet.server.core.haiti.domain.tchala.model.TchalaLang;
import com.tchalanet.server.core.haiti.domain.tchala.model.TchalaNumber;
import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ImportTchalaEntriesCommandHandler
    implements CommandHandler<
        ImportTchalaEntriesCommand,
        com.tchalanet.server.core.haiti.application.command.model.ImportTchalaReport> {

  private static final Logger log = LoggerFactory.getLogger(ImportTchalaEntriesCommandHandler.class);

  private final TchalaImportSourcePort importPort;
  private final TchalaEntryRepositoryPort repo;
  private final Clock clock;
  private final IdGenerator idGenerator;

  public ImportTchalaEntriesCommandHandler(
      TchalaImportSourcePort importPort, TchalaEntryRepositoryPort repo, Clock clock, IdGenerator idGenerator) {
    this.importPort = Objects.requireNonNull(importPort);
    this.repo = Objects.requireNonNull(repo);
    this.clock = Objects.requireNonNull(clock);
    this.idGenerator = Objects.requireNonNull(idGenerator);
  }

  @Override
  public com.tchalanet.server.core.haiti.application.command.model.ImportTchalaReport handle(
      ImportTchalaEntriesCommand command) {
    Objects.requireNonNull(command);
    TchalaLang lang = TchalaLang.of(command.lang());
    List<ImportRow> rows = importPort.readRows(command.payloadRef());
    int total = rows.size();
    int parsed = 0;
    int createdPending = 0;
    int createdCanonical = 0;
    int mergedIntoCanonical = 0;
    int conflicts = 0;
    int duplicatesInFile = 0;

    Set<String> seen = new HashSet<>();
    for (ImportRow importRow : rows) {
      parsed++;
      try {
        DreamText dream = DreamText.of(importRow.dream());
        List<TchalaNumber> numbers = parseNumbers(importRow.numbers());
        String dedupeToken =
            dream.normalizedForKey()
                + "|"
                + numbers.stream().map(n -> n.asTwoDigits()).collect(Collectors.joining(","));
        if (seen.contains(dedupeToken)) {
          duplicatesInFile++;
          continue;
        }
        seen.add(dedupeToken);
        com.tchalanet.server.core.haiti.application.command.model.ImportTchalaEntriesCommand
                .ImportMode
            mode = command.mode();
        Optional<com.tchalanet.server.core.haiti.domain.tchala.model.TchalaEntry> existing =
            repo.findApprovedCanonicalByDedupeKey(
                lang,
                com.tchalanet.server.core.haiti.domain.tchala.model.DedupeKey.from(lang, dream));
        if (existing.isEmpty()) {
          if (mode == ImportTchalaEntriesCommand.ImportMode.DRY_RUN) {
            // only count
            createdPending++;
          } else if (mode == ImportTchalaEntriesCommand.ImportMode.APPLY_AS_PENDING) {
            com.tchalanet.server.core.haiti.domain.tchala.model.TchalaEntry entry =
                com.tchalanet.server.core.haiti.domain.tchala.model.TchalaEntry
                    .newSuggestionFromImport(
                        TchalaEntryId.of(idGenerator.newUuid()),
                        lang, dream, numbers, importRow.note(), Optional.empty(), Instant.now(clock));
            repo.save(entry);
            createdPending++;
          } else {
            com.tchalanet.server.core.haiti.domain.tchala.model.TchalaEntry entry =
                com.tchalanet.server.core.haiti.domain.tchala.model.TchalaEntry.newCanonical(
                    TchalaEntryId.of(idGenerator.newUuid()),
                    lang,
                    dream,
                    numbers,
                    importRow.note(),
                    com.tchalanet.server.core.haiti.domain.tchala.model.TchalaEntrySource.IMPORT,
                    Instant.now(clock));
            repo.save(entry);
            createdCanonical++;
          }
        } else {
          var canonical = existing.get();
          if (canonical.numbers().equals(numbers)) {
            // duplicate of existing canonical
            continue;
          } else {
            // conflict
            if (mode == ImportTchalaEntriesCommand.ImportMode.DRY_RUN) {
              conflicts++;
            } else if (mode == ImportTchalaEntriesCommand.ImportMode.APPLY_AS_PENDING) {
              var pending =
                  com.tchalanet.server.core.haiti.domain.tchala.model.TchalaEntry
                      .newSuggestionFromImport(
                          TchalaEntryId.of(idGenerator.newUuid()),
                          lang,
                          dream,
                          numbers,
                          importRow.note(),
                          Optional.of(canonical.id()),
                          Instant.now(clock));
              repo.save(pending);
              createdPending++;
            } else {
              // merge into canonical using union
              var merged =
                  com.tchalanet.server.core.haiti.domain.tchala.service.TchalaMerge.mergeNumbers(
                      canonical.numbers(),
                      numbers,
                      com.tchalanet.server.core.haiti.domain.tchala.model.MergePolicy
                          .UNION_NUMBERS);
              var newCanon =
                  com.tchalanet.server.core.haiti.domain.tchala.model.TchalaEntry.newCanonical(
                      TchalaEntryId.of(idGenerator.newUuid()),
                      canonical.lang(),
                      canonical.dream(),
                      merged,
                      canonical.note(),
                      canonical.source(),
                      Instant.now(clock));
              repo.save(newCanon);
              mergedIntoCanonical++;
            }
          }
        }
      } catch (Exception ex) {
        log.warn("Import row {} skipped — {}", parsed, ex.getMessage());
      }
    }

    return new com.tchalanet.server.core.haiti.application.command.model.ImportTchalaReport(
        total,
        parsed,
        createdPending,
        createdCanonical,
        mergedIntoCanonical,
        conflicts,
        duplicatesInFile);
  }

  private List<TchalaNumber> parseNumbers(String raw) {
    if (raw == null || raw.isBlank()) return List.of();
    String[] parts = raw.split("[^0-9]+");
    return java.util.Arrays.stream(parts)
        .filter(numberPart -> !numberPart.isBlank())
        .map(numberString -> TchalaNumber.of(Integer.parseInt(numberString)))
        .collect(Collectors.toList());
  }
}
