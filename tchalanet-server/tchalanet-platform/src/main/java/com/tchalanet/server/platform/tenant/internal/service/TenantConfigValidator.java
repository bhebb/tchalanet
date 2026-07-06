package com.tchalanet.server.platform.tenant.internal.service;

import com.tchalanet.server.common.json.utils.JsonUtils;
import com.tchalanet.server.platform.tenant.api.model.view.TenantInternalCommunicationConfig;
import com.tchalanet.server.platform.tenant.api.model.view.TenantInternalSettings;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.time.MonthDay;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

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

    private static final Pattern MONTH_DAY = Pattern.compile("^(0[1-9]|1[0-2])-(0[1-9]|[12][0-9]|3[01])$");

    private static final List<String> TOP_LEVEL_KEYS = List.of(
        "rules", "document", "communication", "locale"
    );

    private final JsonUtils jsonUtils;

    public void validateAll(JsonNode config) {
        if (config == null || config.isNull()) {
            throw new IllegalArgumentException("tenant config must not be null");
        }
        rejectUnknownTopLevelKeys(config);
        requireTopLevelSections(config);
        validateRulesConfig(config);
        validateLocaleConfig(config);
        validateCommunicationConfig(config);
        validateDocumentConfig(config);
    }

    private void rejectUnknownTopLevelKeys(JsonNode config) {
        config.properties().forEach(entry -> {
            if (!TOP_LEVEL_KEYS.contains(entry.getKey())) {
                throw new IllegalArgumentException(
                    "tenant config contains unknown top-level key: '" + entry.getKey() + "'. Allowed: " + TOP_LEVEL_KEYS);
            }
        });
    }

    private void requireTopLevelSections(JsonNode config) {
        for (var section : TOP_LEVEL_KEYS) {
            var value = config.get(section);
            if (value == null || value.isNull() || !value.isObject()) {
                throw new IllegalArgumentException("tenant config section is required: " + section);
            }
        }
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
        if (cal.closedWeekdays() == null) {
            throw new IllegalArgumentException("rules.businessCalendar.closedWeekdays must be an array");
        }
        validateHolidayRules(cal.holidays());
    }

    public void validateLocaleConfig(JsonNode config) {
        var typed = jsonUtils.treeToValue(config, TenantInternalSettings.class);
        if (typed == null || typed.locale() == null) {
            return;
        }
        var locale = typed.locale();
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

    private void validateHolidayRules(
        List<com.tchalanet.server.platform.tenant.api.model.view.TenantBusinessCalendarRules.TenantRecurringHolidayRule> holidays
    ) {
        if (holidays == null) {
            return;
        }
        for (int i = 0; i < holidays.size(); i++) {
            var holiday = holidays.get(i);
            var path = "rules.businessCalendar.holidays[" + i + "]";
            if (holiday == null) {
                throw new IllegalArgumentException(path + " must not be null");
            }
            if (holiday.key() == null || holiday.key().isBlank()) {
                throw new IllegalArgumentException(path + ".key is required");
            }
            if (holiday.monthDay() == null || !MONTH_DAY.matcher(holiday.monthDay()).matches()) {
                throw new IllegalArgumentException(path + ".monthDay must be MM-dd");
            }
            try {
                MonthDay.parse("--" + holiday.monthDay());
            } catch (DateTimeParseException ex) {
                throw new IllegalArgumentException(path + ".monthDay must be a valid month/day", ex);
            }
            if (holiday.open() == null) {
                throw new IllegalArgumentException(path + ".open must be a boolean");
            }
        }
    }
}
