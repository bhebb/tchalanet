package com.tchalanet.server.platform.tenant.internal.service;

import com.tchalanet.server.common.json.utils.JsonUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantConfigValidatorTest {

    private TenantConfigValidator validator;

    /** Full valid config matching the 4 merged classpath fragments. */
    private static final String VALID_CONFIG = """
        {
          "rules": {
            "businessCalendar": {
              "defaultOpen": true,
              "closedWeekdays": [],
              "holidaySalesAllowed": false
            }
          },
          "document": {
            "receipt": {
              "enabled": true,
              "defaultTemplateKey": "sales.ticket.receipt.v1",
              "defaultPaperSize": "RECEIPT_80MM",
              "showQrCode": true
            }
          },
          "communication": {
            "buyerTicketDelivery": {
              "sms":      { "enabled": true,  "amount": 5.00, "currency": "HTG", "paidBy": "BUYER"  },
              "whatsapp": { "enabled": true,  "amount": 5.00, "currency": "HTG", "paidBy": "BUYER"  },
              "email":    { "enabled": true,  "amount": 0.00, "currency": "HTG", "paidBy": "TENANT" }
            }
          },
          "locale": {
            "defaultLanguage": "fr",
            "defaultLocale": "fr-HT",
            "supportedLanguages": ["fr", "ht", "en"],
            "fallbackLanguage": "fr"
          }
        }
        """;

    @BeforeEach
    void setUp() {
        validator = new TenantConfigValidator(new JsonUtils(JsonMapper.builder().build()));
    }

    // ── validateAll ────────────────────────────────────────────────────────

    @Test
    void validConfigPassesAllValidation() {
        assertThatNoException().isThrownBy(() -> validator.validateAll(parse(VALID_CONFIG)));
    }

    @Test
    void nullConfigIsRejected() {
        assertThatThrownBy(() -> validator.validateAll(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must not be null");
    }

    @Test
    void unknownTopLevelKeyIsRejected() {
        var config = VALID_CONFIG.replace("\"locale\":", "\"fees\": {}, \"locale\":");
        assertThatThrownBy(() -> validator.validateAll(parse(config)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("fees");
    }

    // ── validateRulesConfig ────────────────────────────────────────────────

    @Test
    void rulesDefaultOpenNullIsRejected() {
        var config = VALID_CONFIG.replace("\"defaultOpen\": true,", "\"defaultOpen\": null,");
        assertThatThrownBy(() -> validator.validateRulesConfig(parse(config)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("defaultOpen");
    }

    @Test
    void rulesHolidaySalesAllowedNullIsRejected() {
        var config = VALID_CONFIG.replace("\"holidaySalesAllowed\": false", "\"holidaySalesAllowed\": null");
        assertThatThrownBy(() -> validator.validateRulesConfig(parse(config)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("holidaySalesAllowed");
    }

    @Test
    void missingRulesSectionPassesSilently() {
        var config = """
            { "document": { "receipt": { "enabled": false } },
              "communication": {}, "locale": {} }
            """;
        assertThatNoException().isThrownBy(() -> validator.validateRulesConfig(parse(config)));
    }

    // ── validateLocaleConfig ───────────────────────────────────────────────

    @Test
    void localeDefaultLanguageBlankIsRejected() {
        var config = VALID_CONFIG.replace("\"defaultLanguage\": \"fr\"", "\"defaultLanguage\": \"\"");
        assertThatThrownBy(() -> validator.validateLocaleConfig(parse(config)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("defaultLanguage");
    }

    @Test
    void localeSupportedLanguagesEmptyIsRejected() {
        var config = VALID_CONFIG.replace(
            "\"supportedLanguages\": [\"fr\", \"ht\", \"en\"]",
            "\"supportedLanguages\": []");
        assertThatThrownBy(() -> validator.validateLocaleConfig(parse(config)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("supportedLanguages");
    }

    @Test
    void localeFallbackNotInSupportedIsRejected() {
        var config = VALID_CONFIG.replace("\"fallbackLanguage\": \"fr\"", "\"fallbackLanguage\": \"es\"");
        assertThatThrownBy(() -> validator.validateLocaleConfig(parse(config)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("es")
            .hasMessageContaining("supportedLanguages");
    }

    @Test
    void localeValidFallbackInSupportedPasses() {
        assertThatNoException().isThrownBy(() -> validator.validateLocaleConfig(parse(VALID_CONFIG)));
    }

    // ── validateCommunicationConfig ────────────────────────────────────────

    @Test
    void communicationNegativeAmountIsRejected() {
        var config = VALID_CONFIG.replace(
            "\"sms\":      { \"enabled\": true,  \"amount\": 5.00",
            "\"sms\":      { \"enabled\": true,  \"amount\": -1.00");
        assertThatThrownBy(() -> validator.validateCommunicationConfig(parse(config)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("amount");
    }

    @Test
    void communicationInvalidPaidByIsRejected() {
        var config = VALID_CONFIG.replace("\"paidBy\": \"BUYER\"", "\"paidBy\": \"ALIEN\"");
        assertThatThrownBy(() -> validator.validateCommunicationConfig(parse(config)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("paidBy");
    }

    @Test
    void communicationBlankCurrencyIsRejected() {
        var config = VALID_CONFIG.replace(
            "\"currency\": \"HTG\", \"paidBy\": \"BUYER\"  },\n      \"whatsapp\"",
            "\"currency\": \"\",    \"paidBy\": \"BUYER\"  },\n      \"whatsapp\"");
        assertThatThrownBy(() -> validator.validateCommunicationConfig(parse(config)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("currency");
    }

    // ── validateDocumentConfig ─────────────────────────────────────────────

    @Test
    void documentEnabledWithBlankTemplateKeyIsRejected() {
        var config = VALID_CONFIG.replace(
            "\"defaultTemplateKey\": \"sales.ticket.receipt.v1\"",
            "\"defaultTemplateKey\": \"\"");
        assertThatThrownBy(() -> validator.validateDocumentConfig(parse(config)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("defaultTemplateKey");
    }

    @Test
    void documentEnabledWithBlankPaperSizeIsRejected() {
        var config = VALID_CONFIG.replace(
            "\"defaultPaperSize\": \"RECEIPT_80MM\"",
            "\"defaultPaperSize\": \"\"");
        assertThatThrownBy(() -> validator.validateDocumentConfig(parse(config)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("defaultPaperSize");
    }

    @Test
    void documentDisabledSkipsTemplateValidation() {
        var config = VALID_CONFIG
            .replace("\"enabled\": true,", "\"enabled\": false,")
            .replace("\"defaultTemplateKey\": \"sales.ticket.receipt.v1\",", "");
        assertThatNoException().isThrownBy(() -> validator.validateDocumentConfig(parse(config)));
    }

    // ── helpers ────────────────────────────────────────────────────────────

    private tools.jackson.databind.JsonNode parse(String json) {
        return new JsonUtils(JsonMapper.builder().build()).parse(json);
    }
}
