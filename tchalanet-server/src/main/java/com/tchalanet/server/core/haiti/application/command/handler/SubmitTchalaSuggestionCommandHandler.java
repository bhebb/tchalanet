package com.tchalanet.server.core.haiti.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.common.types.id.TchalaEntryId;
import com.tchalanet.server.core.haiti.application.command.model.SubmitTchalaSuggestionCommand;
import com.tchalanet.server.core.haiti.application.command.model.SubmitTchalaSuggestionResult;
import com.tchalanet.server.core.haiti.application.port.out.TchalaEntryRepositoryPort;
import com.tchalanet.server.core.haiti.domain.tchala.model.DedupeKey;
import com.tchalanet.server.core.haiti.domain.tchala.model.DreamText;
import com.tchalanet.server.core.haiti.domain.tchala.model.TchalaEntry;
import com.tchalanet.server.core.haiti.domain.tchala.model.TchalaLang;
import com.tchalanet.server.core.haiti.domain.tchala.model.TchalaNumber;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SubmitTchalaSuggestionCommandHandler
    implements CommandHandler<SubmitTchalaSuggestionCommand, SubmitTchalaSuggestionResult> {

  private final TchalaEntryRepositoryPort repo;
  private final Clock clock;
  private final IdGenerator idGenerator;
  private static final Pattern SPLIT = Pattern.compile("[^0-9]+");

  @Override
  public SubmitTchalaSuggestionResult handle(SubmitTchalaSuggestionCommand command) {
    Objects.requireNonNull(command);
    TchalaLang lang = TchalaLang.of(command.lang());
    DreamText dream = DreamText.of(command.dream());
    List<TchalaNumber> numbers = parseNumbers(command.numbers());
    DedupeKey key = DedupeKey.from(lang, dream);
    Optional<TchalaEntry> canonical = repo.findApprovedCanonicalByDedupeKey(lang, key);
    Optional<java.util.UUID> conflictId = canonical.map(e -> e.id().value());
    Instant now = Instant.now(clock);
    TchalaEntry entry =
        TchalaEntry.newSuggestion(
            TchalaEntryId.of(idGenerator.newUuid()),
            lang,
            dream,
            numbers,
            command.note(),
            canonical.map(TchalaEntry::id),
            now);
    TchalaEntry saved = repo.save(entry);
    return new SubmitTchalaSuggestionResult(
        saved.id().value(), saved.status().name(), canonical.isPresent(), conflictId.orElse(null));
  }

  private List<TchalaNumber> parseNumbers(String raw) {
    if (raw == null || raw.isBlank()) throw new IllegalArgumentException("numbers string required");
    String[] parts = SPLIT.split(raw);
    List<TchalaNumber> out = new ArrayList<>();
    for (String p : parts) {
      if (p == null || p.isBlank()) continue;
      int v = Integer.parseInt(p.trim());
      out.add(TchalaNumber.of(v));
    }
    return out.stream().distinct().collect(Collectors.toList());
  }
}
