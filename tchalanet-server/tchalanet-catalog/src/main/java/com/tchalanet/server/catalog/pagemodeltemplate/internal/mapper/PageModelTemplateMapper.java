package com.tchalanet.server.catalog.pagemodeltemplate.internal.mapper;

import com.tchalanet.server.catalog.pagemodeltemplate.api.model.PageModelTemplateView;
import com.tchalanet.server.catalog.pagemodeltemplate.internal.persistence.PageModelTemplateEntity;
import com.tchalanet.server.common.json.utils.JsonUtils;
import com.tchalanet.server.common.types.id.PageModelTemplateId;
import com.tchalanet.server.common.types.id.TenantId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PageModelTemplateMapper {

    private final JsonUtils jsonUtils;

    public PageModelTemplateView toView(PageModelTemplateEntity e) {
        if (e == null) {
            return null;
        }

        return new PageModelTemplateView(
            PageModelTemplateId.of(e.getId()),
            e.getCode(),
            e.getLogicalId(),
            e.getScope(),
            e.getSlug(),
            e.getName(),
            e.getLabel(),
            e.getDescription(),
            jsonUtils.toJsonNode(e.getSchema()),
            jsonUtils.toJsonNode(e.getModel()),
            e.getSchemaVersion(),
            e.isDefault(),
            e.getLevel(),
            e.getTenantId() == null ? null : TenantId.of(e.getTenantId()),
            e.getCreatedAt(),
            e.getUpdatedAt()
        );
    }

    public List<PageModelTemplateView> toViews(List<PageModelTemplateEntity> list) {
        return list == null ? List.of() : list.stream().map(this::toView).toList();
    }

    public void applyView(PageModelTemplateEntity e, PageModelTemplateView v) {
        e.setCode(requireNonBlank(v.code(), "code"));
        e.setLogicalId(requireNonBlank(v.logicalId(), "logicalId"));
        e.setScope(requireNonBlank(v.scope(), "scope"));
        e.setSlug(requireNonBlank(v.slug(), "slug"));

        e.setName(requireNonBlank(v.name(), "name"));
        e.setLabel(v.label());
        e.setDescription(v.description());

        e.setSchema(jsonUtils.toJson(v.schema()));
        e.setModel(jsonUtils.toJson(v.model()));
        e.setSchemaVersion(v.schemaVersion() == null ? 1 : v.schemaVersion());

        e.setDefault(v.isDefault());
        e.setLevel(v.level());
        e.setTenantId(v.tenantId() == null ? null : v.tenantId().value());
    }

    private String requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("PageModelTemplate " + field + " is required");
        }
        return value.trim();
    }
}
