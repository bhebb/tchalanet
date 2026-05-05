package com.tchalanet.server.core.notification.application.command.model;

/**
 * Résultat de l'envoi d'une notification.
 */
public record SendNotificationResult(
    boolean success,
    String message,
    String idempotencyKey
) {
    public static SendNotificationResult accepted(String idempotencyKey) {
        return new SendNotificationResult(true, "Notification sent successfully", idempotencyKey);
    }

    public static SendNotificationResult failed(String message, String idempotencyKey) {
        return new SendNotificationResult(false, message, idempotencyKey);
    }
}

