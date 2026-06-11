package com.tchalanet.server.platform.contactrequest.internal.service;

import com.tchalanet.server.platform.communication.api.CommunicationApi;
import com.tchalanet.server.platform.communication.api.model.request.SendOutboundMessageRequest;
import com.tchalanet.server.platform.communication.api.model.value.CommunicationChannel;
import com.tchalanet.server.platform.communication.api.model.value.OutboundRecipient;
import com.tchalanet.server.platform.contactrequest.api.ContactRequestApi;
import com.tchalanet.server.platform.contactrequest.api.model.ContactRequestSubmittedView;
import com.tchalanet.server.platform.contactrequest.api.model.SubmitContactRequestCommand;
import com.tchalanet.server.platform.contactrequest.internal.persistence.ContactRequestJpaEntity;
import com.tchalanet.server.platform.contactrequest.internal.persistence.ContactRequestJpaRepository;
import com.tchalanet.server.platform.contactrequest.internal.persistence.ContactRequestReferenceGenerator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContactRequestSubmissionService implements ContactRequestApi {

    private final ContactRequestJpaRepository repository;
    private final ContactRequestReferenceGenerator referenceGenerator;
    private final CommunicationApi communicationApi;
    private final ContactRequestNotifyProperties notifyProperties;

    @Override
    @Transactional
    public ContactRequestSubmittedView submit(SubmitContactRequestCommand command) {
        var result = submitWithNotification(command);
        if (result.notificationFailed()) {
            log.warn("Contact request {} saved but internal notification failed", result.view().requestId());
        }
        return result.view();
    }

    @Transactional
    public SubmitContactResult submitWithNotification(SubmitContactRequestCommand command) {
        var reference = referenceGenerator.nextReference();
        var entity = ContactRequestJpaEntity.create(
            UUID.randomUUID(),
            reference,
            command.intent(),
            command.fullName(),
            command.phone(),
            command.email(),
            command.organizationName(),
            command.city(),
            command.country(),
            command.outletCount(),
            command.preferredContactTime(),
            command.message(),
            command.consentToContact(),
            command.sourcePage());

        repository.save(entity);

        var view = new ContactRequestSubmittedView(
            reference,
            "RECEIVED",
            "Votre demande a été reçue. Notre équipe vous contactera bientôt.");

        boolean notificationFailed = !sendNotification(entity);
        return new SubmitContactResult(view, notificationFailed);
    }

    private boolean sendNotification(ContactRequestJpaEntity entity) {
        if (!notifyProperties.enabled() || notifyProperties.recipients().isEmpty()) {
            return true;
        }
        try {
            for (var recipient : notifyProperties.recipients()) {
                var request = buildEmailRequest(entity, recipient);
                communicationApi.sendNow(request);
            }
            return true;
        } catch (Exception ex) {
            log.error("Failed to send internal notification for contact request {}: {}",
                entity.getReference(), ex.getMessage(), ex);
            return false;
        }
    }

    private SendOutboundMessageRequest buildEmailRequest(ContactRequestJpaEntity entity, String to) {
        var subject = "Nouvelle demande de contact Tchalanet — " + intentLabel(entity);
        var body = buildEmailBody(entity);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("subject", subject);
        metadata.put("body", body);

        return new SendOutboundMessageRequest(
            "contact-request-received",
            CommunicationChannel.EMAIL,
            OutboundRecipient.of(to),
            Locale.FRENCH,
            metadata);
    }

    private String buildEmailBody(ContactRequestJpaEntity entity) {
        return """
            Une nouvelle demande de contact a été reçue.

            Type        : %s
            Nom         : %s
            Téléphone   : %s
            Email       : %s
            Organisation: %s
            Ville/Pays  : %s / %s
            Points vente: %s
            Moment préf : %s

            Message :
            %s

            Référence   : %s
            Source      : %s
            """.formatted(
            intentLabel(entity),
            entity.getFullName(),
            entity.getPhone(),
            orEmpty(entity.getEmail()),
            orEmpty(entity.getOrganizationName()),
            orEmpty(entity.getCity()),
            orEmpty(entity.getCountry()),
            entity.getOutletCount() != null ? entity.getOutletCount() : "-",
            orEmpty(entity.getPreferredContactTime()),
            entity.getMessage(),
            entity.getReference(),
            orEmpty(entity.getSourcePage()));
    }

    private static String intentLabel(ContactRequestJpaEntity entity) {
        return switch (entity.getIntent()) {
            case REQUEST_DEMO -> "Demande de démo";
            case BECOME_OPERATOR -> "Devenir opérateur";
            case SUPPORT -> "Support";
            case PARTNERSHIP -> "Partenariat";
            case OTHER -> "Autre";
        };
    }

    private static String orEmpty(String value) {
        return value != null ? value : "-";
    }
}
