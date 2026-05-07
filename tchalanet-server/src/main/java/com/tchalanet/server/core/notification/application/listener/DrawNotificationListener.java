package com.tchalanet.server.core.notification.application.listener;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.core.draw.domain.event.DrawResultAppliedEvent;
import com.tchalanet.server.core.draw.domain.event.DrawSettledEvent;
import com.tchalanet.server.core.drawresult.domain.event.DrawResultIngestedEvent;
import com.tchalanet.server.core.notification.application.flow.NotificationFlowRouter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listener pour les événements de domaine draw/drawresult.
 * Écoute AFTER_COMMIT et route vers les notifications appropriées.
 *
 * Principes:
 * - Les notifications métier sont envoyées APRÈS le commit de la transaction
 * - Le routing est délégué au NotificationFlowRouter
 * - Les commandes sont envoyées via CommandBus (CQRS)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DrawNotificationListener {

    private final NotificationFlowRouter router;
    private final CommandBus commandBus;

    /**
     * Écoute l'événement DrawResultIngestedEvent (résultat global fetch/ingestion).
     * Envoie des notifications Slack + Email détaillé pour les slots watched (NY/FL).
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDrawResultIngested(DrawResultIngestedEvent event) {
        log.debug("Handling DrawResultIngestedEvent: slotKey={}, drawDate={}, eventId={}",
            event.resultSlotKey(), event.drawDate(), event.eventId().value());

        try {
            var commands = router.routeDrawResultIngested(event);

            if (commands.isEmpty()) {
                log.trace("No notification commands for DrawResultIngested: slot not watched or disabled");
                return;
            }

            for (var command : commands) {
                log.debug("Sending notification command: type={}, recipients={}",
                    command.type(), command.recipients().size());
                commandBus.execute(command);
            }

            log.info("DrawResultIngested notifications sent: slotKey={}, commandCount={}",
                event.resultSlotKey(), commands.size());

        } catch (Exception e) {
            log.error("Failed to send DrawResultIngested notifications: slotKey={}, eventId={}, error={}",
                event.resultSlotKey(), event.eventId().value(), e.getMessage(), e);
            // Do not rethrow - notification failures should not break the business flow
        }
    }

    /**
     * Écoute l'événement DrawResultAppliedEvent (résultat appliqué au draw tenant).
     * Envoie une notification Slack INFO si activée.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDrawResultApplied(DrawResultAppliedEvent event) {
        log.debug("Handling DrawResultAppliedEvent: drawId={}, drawDate={}, eventId={}",
            event.drawId().value(), event.drawDate(), event.eventId().value());

        try {
            var commands = router.routeDrawResultApplied(event);

            if (commands.isEmpty()) {
                log.trace("No notification commands for DrawResultApplied: disabled");
                return;
            }

            for (var command : commands) {
                log.debug("Sending notification command: type={}, recipients={}",
                    command.type(), command.recipients().size());
                commandBus.execute(command);
            }

            log.info("DrawResultApplied notifications sent: drawId={}, commandCount={}",
                event.drawId().value(), commands.size());

        } catch (Exception e) {
            log.error("Failed to send DrawResultApplied notifications: drawId={}, eventId={}, error={}",
                event.drawId().value(), event.eventId().value(), e.getMessage(), e);
            // Do not rethrow - notification failures should not break the business flow
        }
    }

    /**
     * Écoute l'événement DrawSettledEvent (settlement complété).
     * Envoie une notification Slack INFO si activée.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDrawSettled(DrawSettledEvent event) {
        log.debug("Handling DrawSettledEvent: drawId={}, drawDate={}, eventId={}",
            event.drawId().value(), event.drawDate(), event.eventId().value());

        try {
            var commands = router.routeDrawSettled(event);

            if (commands.isEmpty()) {
                log.trace("No notification commands for DrawSettled: disabled");
                return;
            }

            for (var command : commands) {
                log.debug("Sending notification command: type={}, recipients={}",
                    command.type(), command.recipients().size());
                commandBus.execute(command);
            }

            log.info("DrawSettled notifications sent: drawId={}, commandCount={}",
                event.drawId().value(), commands.size());

        } catch (Exception e) {
            log.error("Failed to send DrawSettled notifications: drawId={}, eventId={}, error={}",
                event.drawId().value(), event.eventId().value(), e.getMessage(), e);
            // Do not rethrow - notification failures should not break the business flow
        }
    }
}

