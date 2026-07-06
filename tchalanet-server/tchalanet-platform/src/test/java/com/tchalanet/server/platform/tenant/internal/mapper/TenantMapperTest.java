package com.tchalanet.server.platform.tenant.internal.mapper;

import com.tchalanet.server.common.json.utils.JsonUtils;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.platform.tenant.api.model.TenantStatus;
import com.tchalanet.server.platform.tenant.api.model.TenantType;
import com.tchalanet.server.platform.tenant.internal.domain.TenantConfig;
import com.tchalanet.server.platform.tenant.internal.persistence.TenantJpaEntity;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import tools.jackson.databind.json.JsonMapper;

import java.time.ZoneId;
import java.util.Currency;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TenantMapperTest {

    private static final TenantMapper MAPPER = Mappers.getMapper(TenantMapper.class);
    private static final JsonUtils JSON = new JsonUtils(JsonMapper.builder().build());

    @Test
    void toEntityPersistsProvisionedTenantConfigJson() {
        var config = JSON.parse("""
            {
              "rules": { "businessCalendar": { "defaultOpen": true, "closedWeekdays": [] } },
              "document": { "receipt": { "enabled": true } },
              "communication": {},
              "locale": { "supportedLanguages": ["fr"], "fallbackLanguage": "fr" }
            }
            """);
        var tenant = TenantConfig.createDraft(
            TenantId.of(UUID.randomUUID()),
            "tenant-a",
            "Tenant A",
            TenantType.BORLETTE,
            ZoneId.of("America/Port-au-Prince"),
            Currency.getInstance("HTG"),
            null,
            null,
            null,
            config
        );

        var entity = MAPPER.toEntity(tenant);

        assertThat(entity.getConfig()).isNotBlank();
        assertThat(entity.getDisplayName()).isEqualTo("tenant-a");
        assertThat(JSON.parse(entity.getConfig()).get("document").get("receipt").get("enabled").asText()).isEqualTo("true");
        assertThat(JSON.parse(entity.getConfig()).get("rules").get("businessCalendar").get("defaultOpen").asText()).isEqualTo("true");
    }

    @Test
    void updateEntityDoesNotOverwriteColumnLocaleDefaultsFromJsonConfig() {
        var entity = new TenantJpaEntity();
        entity.setDefaultLanguage("ht");
        entity.setDefaultLocale("ht-HT");

        var config = JSON.parse("""
            {
              "locale": {
                "supportedLanguages": ["en"],
                "fallbackLanguage": "en"
              }
            }
            """);
        var tenant = new TenantConfig(
            TenantId.of(UUID.randomUUID()),
            "tenant-a",
            "Tenant A",
            "Tenant Display",
            TenantType.BORLETTE,
            ZoneId.of("America/Port-au-Prince"),
            Currency.getInstance("HTG"),
            TenantStatus.ACTIVE,
            config,
            null,
            null,
            null
        );

        MAPPER.updateEntity(tenant, entity);

        assertThat(entity.getConfig()).isNotBlank();
        assertThat(entity.getDefaultLanguage()).isEqualTo("ht");
        assertThat(entity.getDefaultLocale()).isEqualTo("ht-HT");
    }
}
