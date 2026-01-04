package com.tchalanet.server.core.draw.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.draw.application.command.model.ApplyExternalResultsForDateCommand;
import com.tchalanet.server.core.draw.application.command.model.ApplyExternalResultsForDateResult;
import com.tchalanet.server.core.draw.application.port.out.DrawApplyPort;
import com.tchalanet.server.core.draw.application.port.out.DrawBatchQueryPort;
import com.tchalanet.server.core.draw.application.port.out.DrawResultReaderPort;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class ApplyExternalResultsForDateCommandHandler
    implements CommandHandler<
        ApplyExternalResultsForDateCommand, ApplyExternalResultsForDateResult> {

  private final DrawBatchQueryPort drawBatchQuery;
  private final DrawResultReaderPort drawResultLookup;
  private final DrawApplyPort drawApply;

  @Override
  public ApplyExternalResultsForDateResult handle(ApplyExternalResultsForDateCommand cmd) {
    validate(cmd);

    var channelCodes =
        cmd.channelCodes().stream()
            .filter(Objects::nonNull)
            .map(s -> s.trim().toUpperCase(Locale.ROOT))
            .filter(s -> !s.isBlank())
            .distinct()
            .toList();

    if (channelCodes.isEmpty()) {
      return new ApplyExternalResultsForDateResult(0, 0, 0, 0);
    }

    List<DrawBatchQueryPort.ClosedDrawRef> closed =
        drawBatchQuery.findClosedDrawsForDate(
            cmd.tenantId(), cmd.drawDate(), channelCodes, cmd.maxDraws());

    int resulted = 0, missing = 0, badQuality = 0, already = 0;

    for (var ref : closed) {
      var resOpt = drawResultLookup.findByChannelCodeAndDate(ref.channelCode(), cmd.drawDate());
      if (resOpt.isEmpty()) {
        missing++;
        continue;
      }

      var res = resOpt.get();
      if (!cmd.force() && !"COMPLETE".equalsIgnoreCase(res.quality())) {
        badQuality++;
        continue;
      }

      if (cmd.dryRun()) {
        resulted++;
        continue;
      }

      var outcome = drawApply.attachResultAndMarkResulted(ref.drawId(), res.id(), cmd.force());
      switch (outcome) {
        case UPDATED -> resulted++;
        case ALREADY_LINKED_OR_NOT_ELIGIBLE -> already++;
      }
    }

    log.info(
        "applyExternalResultsForDate: date={} draws={} resulted={} missing={} badQuality={} already={} force={}",
        cmd.drawDate(),
        closed.size(),
        resulted,
        missing,
        badQuality,
        already,
        cmd.force());

    return new ApplyExternalResultsForDateResult(resulted, missing, badQuality, already);
  }

  private static void validate(ApplyExternalResultsForDateCommand cmd) {
    Objects.requireNonNull(cmd, "command is required");
    Objects.requireNonNull(cmd.drawDate(), "drawDate is required");
    if (cmd.maxDraws() <= 0) throw new IllegalArgumentException("maxDraws must be > 0");
  }
}
