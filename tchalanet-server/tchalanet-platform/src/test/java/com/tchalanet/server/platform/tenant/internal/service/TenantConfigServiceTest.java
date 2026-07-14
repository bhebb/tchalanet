package com.tchalanet.server.platform.tenant.internal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tchalanet.server.catalog.theme.api.ThemeCatalog;
import com.tchalanet.server.catalog.theme.api.ThemePresetView;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.json.utils.JsonUtils;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.ThemePresetId;
import com.tchalanet.server.platform.address.api.AddressApi;
import com.tchalanet.server.platform.tenant.api.TenantPreContextLookupApi;
import com.tchalanet.server.platform.tenant.api.model.TenantType;
import com.tchalanet.server.platform.tenant.api.model.request.GetTenantByIdRequest;
import com.tchalanet.server.platform.tenant.api.model.request.UpdateTenantInternalSettingsSectionRequest;
import com.tchalanet.server.platform.tenant.api.model.request.UpdateTenantInternalSettingsSectionRequest.Section;
import com.tchalanet.server.platform.tenant.api.model.view.TenantInternalSettings;
import com.tchalanet.server.platform.tenant.internal.adapter.TenantPersistenceAdapter;
import com.tchalanet.server.platform.tenant.internal.domain.TenantConfig;
import com.tchalanet.server.platform.tenant.internal.persistence.TenantJpaRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Currency;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class TenantConfigServiceTest {

  private static final String BASE_CONFIG =
      """
        {
          "rules": {
            "promotions": {
              "maryajGratisEnabled": true
            },
            "businessCalendar": {
              "defaultOpen": true,
              "closedWeekdays": [],
              "holidays": []
            }
          },
          "document": {
            "receipt": {
              "enabled": true,
              "defaultTemplateKey": "sales.ticket.receipt.v1",
              "defaultPaperSize": "RECEIPT_80MM",
              "showQrCode": true,
              "headerMessage": null,
              "footerMessage": null
            }
          },
          "communication": {
            "buyerTicketDelivery": {
              "sms": { "enabled": true, "amount": 5.00, "currency": "HTG", "paidBy": "BUYER" },
              "whatsapp": { "enabled": true, "amount": 5.00, "currency": "HTG", "paidBy": "BUYER" },
              "email": { "enabled": true, "amount": 0.00, "currency": "HTG", "paidBy": "TENANT" }
            }
          },
          "locale": {
            "supportedLanguages": ["fr", "ht", "en"],
            "fallbackLanguage": "fr"
          }
        }
        """;

  @Mock private TenantPreContextLookupApi tenantRegistry;
  @Mock private ThemeCatalog themeCatalog;
  @Mock private AddressApi addressApi;
  @Mock private TenantPersistenceAdapter tenants;
  @Mock private TenantJpaRepository tenantRepository;
  @Mock private DomainEventPublisher publisher;
  @Mock private IdGenerator idGenerator;
  @Mock private TenantSettingsReadinessService settingsReadiness;

  private JsonUtils jsonUtils;
  private TenantConfigService service;
  private TenantId tenantId;

  @BeforeEach
  void setUp() {
    jsonUtils = new JsonUtils(JsonMapper.builder().build());
    service =
        new TenantConfigService(
            tenantRegistry,
            themeCatalog,
            addressApi,
            tenants,
            tenantRepository,
            publisher,
            Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneId.of("UTC")),
            idGenerator,
            jsonUtils,
            new TenantConfigValidator(jsonUtils),
            settingsReadiness);
    tenantId = TenantId.of(UUID.fromString("11111111-1111-1111-1111-111111111111"));
  }

  @Test
  void documentSectionPatchPreservesPersistedLocalePolicyAndReceiptTemplate() {
    when(tenants.getRequiredByIdActive(tenantId)).thenReturn(tenantWithConfig(BASE_CONFIG));
    when(tenants.update(any(TenantConfig.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    service.updateTenantInternalSettingsSection(
        new UpdateTenantInternalSettingsSectionRequest(
            tenantId,
            Section.DOCUMENT,
            parse(
                """
                {
                  "receipt": {
                    "headerMessage": "Bon chans",
                    "showQrCode": false
                  }
                }
                """)));

    var settings = savedSettings();

    assertThat(settings.locale().effectiveSupportedLanguages()).containsExactly("fr", "ht", "en");
    assertThat(settings.locale().fallbackLanguage()).isEqualTo("fr");
    assertThat(settings.document().receipt().defaultTemplateKey())
        .isEqualTo("sales.ticket.receipt.v1");
    assertThat(settings.document().receipt().defaultPaperSize()).isEqualTo("RECEIPT_80MM");
    assertThat(settings.document().receipt().headerMessage()).isEqualTo("Bon chans");
    assertThat(settings.document().receipt().showQrCode()).isFalse();
  }

  @Test
  void localeSectionPatchPreservesSupportedLanguagesAndReceiptDefaults() {
    when(tenants.getRequiredByIdActive(tenantId)).thenReturn(tenantWithConfig(BASE_CONFIG));
    when(tenants.update(any(TenantConfig.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    service.updateTenantInternalSettingsSection(
        new UpdateTenantInternalSettingsSectionRequest(
            tenantId,
            Section.LOCALE,
            parse(
                """
                {
                  "fallbackLanguage": "ht"
                }
                """)));

    var settings = savedSettings();

    assertThat(settings.locale().effectiveSupportedLanguages()).containsExactly("fr", "ht", "en");
    assertThat(settings.locale().fallbackLanguage()).isEqualTo("ht");
    assertThat(settings.document().receipt().defaultTemplateKey())
        .isEqualTo("sales.ticket.receipt.v1");
    assertThat(settings.document().receipt().enabled()).isTrue();
    assertThat(settings.document().receipt().showQrCode()).isTrue();
  }

  @Test
  void sectionPatchCompletesLegacyPartialConfigBeforeValidation() {
    when(tenants.getRequiredByIdActive(tenantId))
        .thenReturn(
            tenantWithConfig(
                """
            {
              "communication": {
                "buyerTicketDelivery": {
                  "sms": { "enabled": true, "amount": 5.00, "currency": "HTG", "paidBy": "BUYER" }
                }
              }
            }
            """));
    when(tenants.update(any(TenantConfig.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    service.updateTenantInternalSettingsSection(
        new UpdateTenantInternalSettingsSectionRequest(
            tenantId,
            Section.COMMUNICATION,
            parse(
                """
                {
                  "buyerTicketDelivery": {
                    "sms": { "enabled": true, "amount": 10.00, "currency": "HTG", "paidBy": "BUYER" }
                  }
                }
                """)));

    var settings = savedSettings();

    assertThat(settings.rules().businessCalendar().defaultOpen()).isTrue();
    assertThat(settings.rules().businessCalendar().closedWeekdays()).isEmpty();
    assertThat(settings.document().receipt().defaultTemplateKey())
        .isEqualTo("sales.ticket.receipt.v1");
    assertThat(settings.locale().effectiveSupportedLanguages()).containsExactly("fr", "ht", "en");
    assertThat(settings.communication().buyerTicketDelivery().sms().amount())
        .isEqualByComparingTo("10.00");
  }

  @Test
  void readsPersistedPromotionRulesInsteadOfOnlyDefaults() {
    when(tenants.getRequiredByIdActive(tenantId))
        .thenReturn(
            tenantWithConfig(
                """
            {
              "rules": {
                "promotions": {
                  "maryajGratisEnabled": false
                }
              }
            }
            """));

    var settings = service.getTenantInternalSettings(new GetTenantByIdRequest(tenantId));

    assertThat(settings.rules().promotions().maryajGratisEnabled()).isFalse();
    assertThat(settings.rules().businessCalendar().defaultOpen()).isTrue();
  }

  @Test
  void rulesSectionPatchPreservesPromotionFlag() {
    when(tenants.getRequiredByIdActive(tenantId))
        .thenReturn(
            tenantWithConfig(
                """
            {
              "rules": {
                "promotions": {
                  "maryajGratisEnabled": false
                },
                "businessCalendar": {
                  "defaultOpen": true,
                  "closedWeekdays": [],
                  "holidays": []
                }
              }
            }
            """));
    when(tenants.update(any(TenantConfig.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    service.updateTenantInternalSettingsSection(
        new UpdateTenantInternalSettingsSectionRequest(
            tenantId,
            Section.RULES,
            parse(
                """
                {
                  "businessCalendar": {
                    "closedWeekdays": ["SUNDAY"]
                  }
                }
                """)));

    var settings = savedSettings();

    assertThat(settings.rules().promotions().maryajGratisEnabled()).isFalse();
    assertThat(settings.rules().businessCalendar().closedWeekdays())
        .containsExactly(java.time.DayOfWeek.SUNDAY);
  }

  @Test
  void resolveActiveThemeIdUsesTchalanetThemeWhenNoThemeIsRequested() {
    var themeId = ThemePresetId.of(UUID.fromString("22222222-2222-2222-2222-222222222222"));
    when(themeCatalog.findByCode("tchalanet"))
        .thenReturn(Optional.of(activeTheme(themeId, "tchalanet")));

    assertThat(service.resolveActiveThemeId(null)).isEqualTo(themeId);
  }

  private TenantInternalSettings savedSettings() {
    var captor = ArgumentCaptor.forClass(TenantConfig.class);
    verify(tenants).update(captor.capture());
    return jsonUtils.treeToValue(captor.getValue().config(), TenantInternalSettings.class);
  }

  private TenantConfig tenantWithConfig(String config) {
    return TenantConfig.createDraft(
            tenantId,
            "acme",
            "ACME",
            TenantType.BORLETTE,
            ZoneId.of("America/Port-au-Prince"),
            Currency.getInstance("HTG"),
            null,
            null,
            BigDecimal.ZERO,
            parse(config))
        .activate(Instant.parse("2026-01-01T00:00:00Z"));
  }

  private ThemePresetView activeTheme(ThemePresetId id, String code) {
    return new ThemePresetView(
        id, code, "tchalanet", JsonUtils.emptyObject(), null, true, true, null, null);
  }

  private JsonNode parse(String json) {
    return jsonUtils.parse(json);
  }
}
