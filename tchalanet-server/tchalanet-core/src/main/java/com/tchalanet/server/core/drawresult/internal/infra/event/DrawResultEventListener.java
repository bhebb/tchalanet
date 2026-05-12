package com.tchalanet.server.core.drawresult.internal.infra.event;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.platform.idempotence.api.ProcessedEventPort;
import com.tchalanet.server.core.draw.internal.domain.event.DrawResultCorrectedEvent;
import com.tchalanet.server.core.drawresult.api.command.MarkDrawResultOverriddenCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Clock;

/**
 * Listener pour les événements liés aux DrawResults.
 *
 * Gère la mise à jour automatique du status des DrawResults suite à des événements
 * du module draw.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DrawResultEventListener {

    private static final String KEY_MARK_OVERRIDDEN = "drawresult.mark_overridden";

    private final ProcessedEventPort processedEventPort;
    private final CommandBus commandBus;
    private final Clock clock;

    /**
     * Écoute l'événement DrawResultCorrectedEvent et déclenche le marquage du previous DrawResult comme OVERRIDDEN.
     *
     * Étape 9 du flow de correction de résultat.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDrawResultCorrected(DrawResultCorrectedEvent event) {
        // Idempotency check
        if (!processedEventPort.markProcessedIfAbsent(KEY_MARK_OVERRIDDEN, event.eventId().value())) {
            log.debug("DrawResultCorrectedEvent already processed: eventId={}", event.eventId());
            return;
        }

        // Délègue au handler via CommandBus
        commandBus.execute(new MarkDrawResultOverriddenCommand(
            event.previousDrawResultId(),
            event.reason(),
            clock.instant()
        ));
    }
}

