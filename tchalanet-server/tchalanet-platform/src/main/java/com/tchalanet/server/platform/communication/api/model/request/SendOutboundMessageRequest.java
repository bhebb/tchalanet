package com.tchalanet.server.platform.communication.api.model.request;

import com.tchalanet.server.platform.communication.api.model.value.CommunicationChannel;
import com.tchalanet.server.platform.communication.api.model.value.MessagePriority;
import com.tchalanet.server.platform.communication.api.model.value.OutboundRecipient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public record SendOutboundMessageRequest(
    String type,
    CommunicationChannel channel,
    OutboundRecipient recipient,
    Locale locale,
    Map<String, Object> metadata,
    List<OutboundAttachment> attachments) {

    public SendOutboundMessageRequest(
        String type,
        CommunicationChannel channel,
        OutboundRecipient recipient,
        Locale locale,
        Map<String, Object> metadata
    ) {
        this(type, channel, recipient, locale, metadata, List.of());
    }

    public SendOutboundMessageRequest {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        attachments = attachments == null ? List.of() : List.copyOf(attachments);
    }

    public String correlationKey() {
        return optionalString("correlationKey", optionalString("idempotencyKey", null));
    }

    public String subject() {
        return optionalString("subject", optionalString("title", null));
    }

    public String body() {
        return optionalString("body", optionalString("message", ""));
    }

    public String templateKey() {
        return optionalString("templateKey", type);
    }

    public MessagePriority priority() {
        var raw = optionalString("priority", "NORMAL");

        try {
            return MessagePriority.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return MessagePriority.NORMAL;
        }
    }

    public Map<String, Object> metadataWithAttachments() {
        if (attachments.isEmpty()) {
            return metadata;
        }
        var enriched = new LinkedHashMap<String, Object>(metadata);
        enriched.put("attachments", attachments.stream()
            .map(OutboundAttachment::toMetadata)
            .toList());
        return Map.copyOf(enriched);
    }

    private String optionalString(String key, String defaultValue) {
        var value = metadata.get(key);
        if (value == null) {
            return defaultValue;
        }

        var stringValue = value.toString().trim();
        return stringValue.isBlank() ? defaultValue : stringValue;
    }
}
