package com.tchalanet.server.platform.tenant.internal.service;

import com.tchalanet.server.common.json.utils.JsonUtils;
import com.tchalanet.server.platform.tenant.api.model.view.TenantInternalCommunicationConfig;
import com.tchalanet.server.platform.tenant.api.model.view.TenantInternalSettings;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.util.Locale;
import java.util.Set;

/**
 * Validates the tenant config JSONB structure for all 4 sections:
 * rules, document, communication, locale.
 *
 * Called both on tenant creation (full validation) and on settings update.
 * All validation throws {@link IllegalArgumentException} on violation.
 */
@Component
@RequiredArgsConstructor
public class TenantConfigValidator {

    private static final Set<String> ALLOWED_TOP_LEVEL_KEYS = Set.of(
        "rules", "document", "communication", "locale"
    );

    private final JsonUtils jsonUtils;

    public void validateAll(JsonNode config) {
        if (config == null || config.isNull()) {
            throw new IllegalArgumentException("tenant config must not be null");
        }
        rejectUnknownTopLevelKeys(config);
        validateRulesConfig(config);
        validateLocaleConfig(config);
        validateCommunicationConfig(config);
        validateDocumentConfig(config);
    }

    private void rejectUnknownTopLevelKeys(JsonNode config) {
        config.properties().forEach(entry -> {
            if (!ALLOWED_TOP_LEVEL_KEYS.contains(entry.getKey())) {
                throw new IllegalArgumentException(
                    "tenant config contains unknown top-level key: '" + entry.getKey() + "'. Allowed: " + ALLOWED_TOP_LEVEL_KEYS);
            }
        });
    }

    public void validateRulesConfig(JsonNode config) {
        var typed = jsonUtils.treeToValue(config, TenantInternalSettings.class);
        if (typed == null || typed.rules() == null) {
            return;
        }
        var rules = typed.rules();
        var cal = rules.businessCalendar();
        if (cal == null) {
            return;
        }
        if (cal.defaultOpen() == null) {
            throw new IllegalArgumentException("rules.businessCalendar.defaultOpen must be a boolean");
        }
        if (cal.holidaySalesAllowed() == null) {
            throw new IllegalArgumentException("rules.businessCalendar.holidaySalesAllowed must be a boolean");
        }
    }

    public void validateLocaleConfig(JsonNode config) {
        var typed = jsonUtils.treeToValue(config, TenantInternalSettings.class);
        if (typed == null || typed.locale() == null) {
            return;
        }
        var locale = typed.locale();
        if (locale.defaultLanguage() == null || locale.defaultLanguage().isBlank()) {
            throw new IllegalArgumentException("locale.defaultLanguage is required");
        }
        if (locale.defaultLocale() == null || locale.defaultLocale().isBlank()) {
            throw new IllegalArgumentException("locale.defaultLocale is required");
        }
        var supported = locale.effectiveSupportedLanguages();
        if (supported.isEmpty()) {
            throw new IllegalArgumentException("locale.supportedLanguages must not be empty");
        }
        if (locale.fallbackLanguage() == null || locale.fallbackLanguage().isBlank()) {
            throw new IllegalArgumentException("locale.fallbackLanguage is required");
        }
        if (!supported.contains(locale.fallbackLanguage())) {
            throw new IllegalArgumentException(
                "locale.fallbackLanguage '" + locale.fallbackLanguage() + "' must be in supportedLanguages " + supported);
        }
    }

    public void validateCommunicationConfig(JsonNode config) {
        var typed = jsonUtils.treeToValue(config, TenantInternalSettings.class);
        if (typed == null || typed.communication() == null || typed.communication().buyerTicketDelivery() == null) {
            return;
        }
        var delivery = typed.communication().buyerTicketDelivery();
        validateDeliveryChannel("communication.buyerTicketDelivery.sms", delivery.sms());
        validateDeliveryChannel("communication.buyerTicketDelivery.whatsapp", delivery.whatsapp());
        validateDeliveryChannel("communication.buyerTicketDelivery.email", delivery.email());
    }

    public void validateDocumentConfig(JsonNode config) {
        var typed = jsonUtils.treeToValue(config, TenantInternalSettings.class);
        if (typed == null || typed.document() == null || typed.document().receipt() == null) {
            return;
        }
        var receipt = typed.document().receipt();
        if (!Boolean.TRUE.equals(receipt.enabled())) {
            return;
        }
        if (receipt.defaultTemplateKey() == null || receipt.defaultTemplateKey().isBlank()) {
            throw new IllegalArgumentException("document.receipt.defaultTemplateKey is required");
        }
        if (receipt.defaultPaperSize() == null || receipt.defaultPaperSize().isBlank()) {
            throw new IllegalArgumentException("document.receipt.defaultPaperSize is required");
        }
    }

    private void validateDeliveryChannel(String path, TenantInternalCommunicationConfig.DeliveryChannelConfig channel) {
        if (channel == null) {
            return;
        }
        if (channel.amount() == null || channel.amount().signum() < 0) {
            throw new IllegalArgumentException(path + ".amount must be >= 0");
        }
        if (channel.currency() == null || channel.currency().isBlank()) {
            throw new IllegalArgumentException(path + ".currency is required");
        }
        if (channel.paidBy() == null || channel.paidBy().isBlank()) {
            throw new IllegalArgumentException(path + ".paidBy is required");
        }
        var paidBy = channel.paidBy().trim().toUpperCase(Locale.ROOT);
        if (!"BUYER".equals(paidBy) && !"TENANT".equals(paidBy) && !"SELLER".equals(paidBy)) {
            throw new IllegalArgumentException(path + ".paidBy must be BUYER, TENANT, or SELLER");
        }
    }
}
