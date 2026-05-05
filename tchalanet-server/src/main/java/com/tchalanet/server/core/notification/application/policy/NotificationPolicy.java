package com.tchalanet.server.core.notification.application.policy;

import com.tchalanet.server.core.notification.domain.InvalidNotificationException;
import com.tchalanet.server.core.notification.domain.model.NotificationChannel;
import com.tchalanet.server.core.notification.domain.model.NotificationRecipient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Politique de validation pour l'envoi de notifications.
 * Valide les règles spécifiques à chaque canal et empêche les abus.
 */
@Component
@Slf4j
public class NotificationPolicy {

    /**
     * Valide qu'une liste de destinataires respecte les règles métier.
     *
     * @param recipients destinataires à valider
     * @throws InvalidNotificationException si la validation échoue
     */
    public void validateRecipients(List<NotificationRecipient> recipients) {
        if (recipients == null || recipients.isEmpty()) {
            throw new InvalidNotificationException("At least one recipient is required");
        }

        for (var recipient : recipients) {
            validateRecipient(recipient);
        }
    }

    /**
     * Valide un destinataire individuel selon son canal.
     */
    private void validateRecipient(NotificationRecipient recipient) {
        switch (recipient.channel()) {
            case SLACK -> validateSlackRecipient(recipient);
            case EMAIL -> validateEmailRecipient(recipient);
            case SMS -> validateSmsRecipient(recipient);
            case WHATSAPP -> validateWhatsAppRecipient(recipient);
            case WEB -> validateWebRecipient(recipient);
            case PUSH -> validatePushRecipient(recipient);
        }
    }

    private void validateSlackRecipient(NotificationRecipient recipient) {
        if (recipient.channelKey() == null || recipient.channelKey().isBlank()) {
            throw new InvalidNotificationException("SLACK requires channelKey");
        }
    }

    private void validateEmailRecipient(NotificationRecipient recipient) {
        if (recipient.to() == null || recipient.to().isBlank()) {
            throw new InvalidNotificationException("EMAIL requires to (email address)");
        }
        if (!isValidEmail(recipient.to())) {
            throw new InvalidNotificationException("Invalid email format: " + recipient.to());
        }
    }

    private void validateSmsRecipient(NotificationRecipient recipient) {
        if (recipient.to() == null || recipient.to().isBlank()) {
            throw new InvalidNotificationException("SMS requires to (phone number)");
        }
        if (!isValidPhone(recipient.to())) {
            throw new InvalidNotificationException("Invalid phone format (must start with +): " + recipient.to());
        }
    }

    private void validateWhatsAppRecipient(NotificationRecipient recipient) {
        if (recipient.to() == null || recipient.to().isBlank()) {
            throw new InvalidNotificationException("WHATSAPP requires to (phone number)");
        }
        if (!isValidPhone(recipient.to())) {
            throw new InvalidNotificationException("Invalid phone format (must start with +): " + recipient.to());
        }
    }

    private void validateWebRecipient(NotificationRecipient recipient) {
        // TODO: Future - validate tenantId/userId for web notifications
        log.debug("WEB notification validation - not yet fully implemented");
    }

    private void validatePushRecipient(NotificationRecipient recipient) {
        // TODO: Future - validate device token for push notifications
        log.debug("PUSH notification validation - not yet fully implemented");
    }

    private boolean isValidEmail(String email) {
        // Simple validation - can be improved with a proper email validator
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    private boolean isValidPhone(String phone) {
        // Must start with + and have 6-15 digits
        return phone.matches("\\+[0-9]{6,15}");
    }

    /**
     * Vérifie si un use case peut envoyer des notifications.
     * Pour l'instant, toujours true. Future: vérifier tenant preferences, opt-in, etc.
     */
    public boolean canSend(String useCase) {
        // TODO: Future - check tenant notification preferences
        // TODO: Future - check opt-in/consent for client notifications
        return true;
    }
}

