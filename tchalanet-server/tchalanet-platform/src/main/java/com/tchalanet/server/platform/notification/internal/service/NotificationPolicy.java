package com.tchalanet.server.platform.notification.internal.service;

import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.platform.notification.api.error.NotificationErrorCodes;
import com.tchalanet.server.platform.notification.api.model.NotificationRecipient;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Politique de validation pour l'envoi de notifications. Valide les règles spécifiques à chaque
 * canal et empêche les abus.
 */
@Component
@Slf4j
public class NotificationPolicy {

  /**
   * Valide qu'une liste de destinataires respecte les règles métier.
   *
   * @param recipients destinataires à valider
   * @throws com.tchalanet.server.common.web.error.ProblemRestException si la validation échoue
   */
  public void validateRecipients(List<NotificationRecipient> recipients) {
    if (recipients == null || recipients.isEmpty()) {
      throw ProblemRest.of(NotificationErrorCodes.RECIPIENTS_REQUIRED);
    }

    for (var recipient : recipients) {
      validateRecipient(recipient);
    }
  }

  /** Valide un destinataire individuel selon son canal. */
  private void validateRecipient(NotificationRecipient recipient) {
    switch (recipient.channel()) {
      case SLACK -> validateSlackRecipient(recipient);
      case EMAIL -> validateEmailRecipient(recipient);
      case SMS -> validateSmsRecipient(recipient);
      case WHATSAPP -> validateWhatsAppRecipient(recipient);
      case WEB -> validateWebRecipient(recipient);
      case PUSH -> validatePushRecipient(recipient);
      default -> log.warn("Unknown notification channel: {}", recipient.channel());
    }
  }

  private void validateSlackRecipient(NotificationRecipient recipient) {
    String channelKey = recipient.channelKey();
    if (channelKey == null || channelKey.isBlank()) {
      throw ProblemRest.of(NotificationErrorCodes.SLACK_CHANNEL_KEY_REQUIRED);
    }
  }

  private void validateEmailRecipient(NotificationRecipient recipient) {
    String to = recipient.to();
    if (to == null || to.isBlank()) {
      throw ProblemRest.of(NotificationErrorCodes.EMAIL_REQUIRED);
    }
    if (!isValidEmail(to)) {
      throw ProblemRest.of(NotificationErrorCodes.EMAIL_INVALID);
    }
  }

  private void validateSmsRecipient(NotificationRecipient recipient) {
    String to = recipient.to();
    if (to == null || to.isBlank()) {
      throw ProblemRest.of(NotificationErrorCodes.PHONE_REQUIRED);
    }
    if (!isValidPhone(to)) {
      throw ProblemRest.of(NotificationErrorCodes.PHONE_INVALID);
    }
  }

  private void validateWhatsAppRecipient(NotificationRecipient recipient) {
    String to = recipient.to();
    if (to == null || to.isBlank()) {
      throw ProblemRest.of(NotificationErrorCodes.PHONE_REQUIRED);
    }
    if (!isValidPhone(to)) {
      throw ProblemRest.of(NotificationErrorCodes.PHONE_INVALID);
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
   * Vérifie si un use case peut envoyer des notifications. Pour l'instant, toujours true. Future:
   * vérifier tenant preferences, opt-in, etc.
   */
  public boolean canSend(String useCase) {
    // TODO: Future - check tenant notification preferences
    // TODO: Future - check opt-in/consent for client notifications
    return true;
  }
}
