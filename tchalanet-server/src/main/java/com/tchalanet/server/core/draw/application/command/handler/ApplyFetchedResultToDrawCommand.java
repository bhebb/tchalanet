package com.tchalanet.server.core.draw.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.time.DateWindows;
import com.tchalanet.server.common.time.OccurredAtResolver;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.core.draw.application.command.model.ApplyExternalResultsWindowCommand;
import com.tchalanet.server.core.draw.application.command.model.ApplyExternalResultsWindowResult;
import com.tchalanet.server.core.draw.application.port.out.DrawApplyPort;
import com.tchalanet.server.core.drawresult.api.DrawResultCatalogBack;
import com.tchalanet.server.core.drawresult.domain.event.DrawResultedEvent;
import com.tchalanet.server.core.drawresult.infra.config.DrawResultsProperties;
import com.tchalanet.server.catalog.resultslot.api.ResultSlotCatalog;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@Slf4j
@RequiredArgsConstructor
public class ApplyFetchedResultToDrawCommand
    implements CommandHandler<ApplyExternalResultsWindowCommand, ApplyExternalResultsWindowResult> {

  private final DrawResultCatalogBack drawResultCatalog;
  private final ResultSlotCatalog resultSlotReader;
  private final DrawApplyPort drawApply;
  private final DrawResultsProperties props;
  private final Clock clock;
  private final DomainEventPublisher publisher;

  @Override
  public ApplyExternalResultsWindowResult handle(ApplyExternalResultsWindowCommand cmd) {
    validate(cmd);

    int applied = 0;
    int alreadyOrNotEligible = 0;
    int skippedDryRun = 0;
    int notFound = 0;
    int errors = 0;

    int daysBack = clampDaysBack(cmd.daysBack());

    for (LocalDate date : DateWindows.datesBackInclusive(cmd.baseDate(), daysBack)) {
      for (String slotKeyRaw : cmd.slotKeys()) {
        final String slotKey = normalizeKey(slotKeyRaw);

        try {
          var slotOpt = resultSlotReader.findBySlotKey(slotKey);
          if (slotOpt.isEmpty()) {
            notFound++;
            continue;
          }
          var slot = slotOpt.get();
          if (!slot.active()) {
            notFound++;
            continue;
          }

          // deterministic occurredAt for the slot/date
          Instant occurredAt =
              OccurredAtResolver.resolve(null, date, slot.drawTime(), slot.timezone(), clock);

          var drawResultIdOpt =
              drawResultCatalog.findByResulSlotIdAndOccurredAt(slot.id(), occurredAt);
          if (drawResultIdOpt.isEmpty()) {
            notFound++;
            continue;
          }

          if (cmd.dryRun()) {
            skippedDryRun++;
            continue;
          }

          var drawResultId = drawResultIdOpt.get();
          var outcome =
              drawApply.attachResultBySlot(
                  cmd.tenantId(), date, slot.id(), drawResultId, cmd.force());

          if (outcome == DrawApplyPort.ApplyOutcome.UPDATED) {
            applied++;

            // Keep your current event type signature for compatibility.
            // Recommended next: include resultSlotId + occurredAt (+ drawId via RETURNING).
            var event =
                new DrawResultedEvent(
                    java.util.UUID.randomUUID(),
                    Instant.now(clock),
                    cmd.tenantId(),
                    date,
                    slot.slotKey(),
                    drawResultId.uuid());

            AfterCommit.run(() -> publisher.publish(event));

          } else {
            alreadyOrNotEligible++;
          }

        } catch (Exception e) {
          errors++;
          log.warn("draw-results.apply failed slot={} date={} err={}", slotKey, date, e.toString());
        }
      }
    }

    return new ApplyExternalResultsWindowResult(
        applied,
        /* updated */ 0,
        /* already */ alreadyOrNotEligible,
        /* skipped */ skippedDryRun,
        /* notFound */ notFound,
        /* errors */ errors);
  }

  private int clampDaysBack(int v) {
    int x = Math.max(0, v);
    return Math.min(x, props.getLimits().getHardDaysBack());
  }

  private static void validate(ApplyExternalResultsWindowCommand cmd) {
    Objects.requireNonNull(cmd, "command is required");
    Objects.requireNonNull(cmd.tenantId(), "tenantId required");
    Objects.requireNonNull(cmd.baseDate(), "baseDate is required");
    if (cmd.daysBack() < 0) throw new IllegalArgumentException("daysBack must be >= 0");
    if (cmd.slotKeys() == null || cmd.slotKeys().isEmpty())
      throw new IllegalArgumentException("slotKeys required");
    if (cmd.maxSlots() <= 0) throw new IllegalArgumentException("maxSlots must be > 0");
  }

  private static String normalizeKey(String s) {
    return s == null ? "" : s.trim().toUpperCase(java.util.Locale.ROOT);
  }
}
