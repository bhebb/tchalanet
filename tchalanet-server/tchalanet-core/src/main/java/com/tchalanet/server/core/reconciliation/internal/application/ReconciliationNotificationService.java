package com.tchalanet.server.core.reconciliation.internal.application;

import com.tchalanet.server.common.types.id.ReconciliationRunId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.platform.communication.api.CommunicationApi;
import com.tchalanet.server.platform.communication.api.CommunicationSettingsApi;
import com.tchalanet.server.platform.communication.api.model.request.OutboundAttachment;
import com.tchalanet.server.platform.communication.api.model.request.SendOutboundMessageRequest;
import com.tchalanet.server.platform.communication.api.model.value.CommunicationChannel;
import com.tchalanet.server.platform.communication.api.model.value.MessagePriority;
import com.tchalanet.server.platform.communication.api.model.value.OutboundRecipient;
import com.tchalanet.server.platform.tenantconfig.api.TenantConfigApi;
import com.tchalanet.server.platform.tenantconfig.api.model.request.GetTenantByIdRequest;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReconciliationNotificationService {

    private static final String TYPE = "reconciliation.daily.anomalies";

    private final ReconciliationAnomalyCsvExporter csvExporter;
    private final CommunicationApi communicationApi;
    private final CommunicationSettingsApi communicationSettingsApi;
    private final TenantConfigApi tenantConfigApi;

    public ReconciliationNotificationService(
        ReconciliationAnomalyCsvExporter csvExporter,
        CommunicationApi communicationApi,
        CommunicationSettingsApi communicationSettingsApi,
        TenantConfigApi tenantConfigApi
    ) {
        this.csvExporter = csvExporter;
        this.communicationApi = communicationApi;
        this.communicationSettingsApi = communicationSettingsApi;
        this.tenantConfigApi = tenantConfigApi;
    }

    @Transactional(readOnly = true)
    public void enqueueAnomalyEmail(ReconciliationRunNotification notification) {
        if (notification.criticalCount() == 0 && notification.highCount() == 0) {
            return;
        }
        var settings = communicationSettingsApi.getTenantSettings(notification.tenantId());
        if (!settings.emailEnabled()) {
            return;
        }

        var recipients = recipients(settings.criticalAlertEmail(), settings.opsAlertEmail());
        if (recipients.isEmpty()) {
            return;
        }

        var tenant = tenantConfigApi.getTenantById(new GetTenantByIdRequest(notification.tenantId()));
        var tenantCode = tenant.code();
        var tenantName = tenant.name();
        var csv = csvExporter.export(notification.tenantId(), tenantCode, notification.runId());
        var attachment = new OutboundAttachment(
            "reconciliation-anomalies_%s_%s_%s.csv".formatted(
                tenantCode,
                notification.businessDate(),
                notification.runId().value()),
            "text/csv; charset=UTF-8",
            csv);
        var locale = Locale.forLanguageTag(settings.defaultLocale() == null ? "fr" : settings.defaultLocale());
        for (var recipient : recipients) {
            communicationApi.enqueue(new SendOutboundMessageRequest(
                TYPE,
                CommunicationChannel.EMAIL,
                new OutboundRecipient(notification.tenantId(), null, recipient, null),
                locale,
                Map.of(
                    "subject", subject(tenantName, notification.businessDate()),
                    "body", body(tenantName, notification),
                    "priority", MessagePriority.HIGH.name(),
                    "correlationKey", "reconciliation:%s:%s".formatted(notification.runId().value(), recipient)
                ),
                List.of(attachment)));
        }
    }

    private LinkedHashSet<String> recipients(String criticalAlertEmail, String opsAlertEmail) {
        var recipients = new LinkedHashSet<String>();
        addEmail(recipients, criticalAlertEmail);
        addEmail(recipients, opsAlertEmail);
        return recipients;
    }

    private void addEmail(LinkedHashSet<String> recipients, String email) {
        if (email != null && !email.isBlank()) {
            recipients.add(email.trim());
        }
    }

    private String subject(String tenantName, LocalDate businessDate) {
        return "[Tchalanet] Anomalies de reconciliation detectees - %s - %s"
            .formatted(tenantName, businessDate);
    }

    private String body(String tenantName, ReconciliationRunNotification notification) {
        return """
            Bonjour,

            Le batch de reconciliation quotidien a detecte des anomalies pour le tenant %s.

            Date metier : %s
            Run ID : %s
            Statut du run : COMPLETED_WITH_ANOMALIES

            Resume :
            - Draws verifies : %d
            - Tickets verifies : %d
            - Anomalies totales : %d
            - Anomalies critiques : %d
            - Anomalies high : %d
            - Anomalies medium : %d

            Les details sont disponibles dans le fichier CSV joint.

            Aucune correction automatique n'a ete appliquee.
            Les anomalies doivent etre revues par un administrateur autorise avant toute action de reparation.

            Cordialement,
            Tchalanet
            """.formatted(
            tenantName,
            notification.businessDate(),
            notification.runId().value(),
            notification.checkedDrawCount(),
            notification.checkedTicketCount(),
            notification.anomalyCount(),
            notification.criticalCount(),
            notification.highCount(),
            notification.mediumCount());
    }

    public record ReconciliationRunNotification(
        ReconciliationRunId runId,
        TenantId tenantId,
        LocalDate businessDate,
        long checkedDrawCount,
        long checkedTicketCount,
        long anomalyCount,
        long criticalCount,
        long highCount,
        long mediumCount
    ) {}
}
