package com.tchalanet.server.platform.tenant.api.model.request;

import com.tchalanet.server.common.types.id.TenantId;
import tools.jackson.databind.JsonNode;

public record UpdateTenantInternalSettingsSectionRequest(
    TenantId tenantId,
    Section section,
    JsonNode value
) {
    public UpdateTenantInternalSettingsSectionRequest {
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId is required");
        }
        if (section == null) {
            throw new IllegalArgumentException("section is required");
        }
        if (value == null || value.isNull()) {
            throw new IllegalArgumentException("value is required");
        }
        if (!value.isObject()) {
            throw new IllegalArgumentException("value must be a JSON object");
        }
    }

    public enum Section {
        COMMUNICATION("communication"),
        DOCUMENT("document"),
        RULES("rules"),
        LOCALE("locale");

        private final String jsonKey;

        Section(String jsonKey) {
            this.jsonKey = jsonKey;
        }

        public String jsonKey() {
            return jsonKey;
        }

        public static Section fromJsonKey(String raw) {
            if (raw == null || raw.isBlank()) {
                throw new IllegalArgumentException("section is required");
            }
            var normalized = raw.trim();
            for (var section : values()) {
                if (section.jsonKey.equalsIgnoreCase(normalized) || section.name().equalsIgnoreCase(normalized)) {
                    return section;
                }
            }
            throw new IllegalArgumentException("Unsupported tenant settings section: " + raw);
        }
    }
}
