package com.tchalanet.server.core.draw.application.command.handler;

import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.draw.application.command.model.FetchAndApplyExternalResultCommand;
import com.tchalanet.server.core.draw.application.port.in.command.FetchAndApplyExternalResultCommandHandler;
import com.tchalanet.server.core.draw.application.port.out.DrawReaderPort;
import com.tchalanet.server.core.draw.application.port.out.DrawResultWriterPort;
import com.tchalanet.server.core.draw.application.port.out.DrawWriterPort;
import com.tchalanet.server.core.draw.application.port.out.ExternalDrawResultPort;
import com.tchalanet.server.core.draw.domain.model.DrawResult;
import com.tchalanet.server.core.draw.domain.model.DrawSource;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Use case pour récupérer les résultats d’un tirage via les providers externes / saisies manuelles
 * et les enregistrer.
 *
 * <p>TODO: brancher les ports out (ExternalDrawResultPort, DrawReader/WriterPort, etc.) et
 * implémenter la logique métier complète.
 */
@UseCase
@RequiredArgsConstructor
@Slf4j
public class FetchAndApplyExternalResultUseCase
    implements FetchAndApplyExternalResultCommandHandler {

  private final DrawReaderPort drawReaderPort;
  private final DrawWriterPort drawWriterPort;
  private final DrawResultWriterPort drawResultWriterPort;
  private final ExternalDrawResultPort externalDrawResultPort;

  @Override
  @TchTx
  public void handle(FetchAndApplyExternalResultCommand command) {
    var tenantId = command.tenantId();
    var drawId = command.drawId();

    var draw =
        drawReaderPort
            .findById(tenantId, drawId)
            .orElseThrow(() -> new IllegalArgumentException("Draw not found: " + drawId));

    var external = externalDrawResultPort.fetchExternalResult(tenantId, drawId);

    var result =
        new DrawResult(
            DrawSource.EXTERNAL,
            external.numbers(),
            external.numbersExtra(),
            external.occurredAt() != null ? external.occurredAt() : Instant.now(),
            external.rawPayload(),
            false,
            null);

    draw.applyResult(result);

    drawResultWriterPort.save(tenantId, drawId, result);
    drawWriterPort.save(draw);

    // éventuellement publier DrawResultedEvent ici
  }
}
