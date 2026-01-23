package com.tchalanet.server.catalog.theme.internal.mapper;

import com.tchalanet.server.catalog.theme.api.ThemePresetView;
import com.tchalanet.server.catalog.theme.internal.persistence.ThemePresetJpaEntity;
import com.tchalanet.server.common.mapper.CommonIdMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mapstruct.Mapper;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring", uses = {CommonIdMapper.class})
public abstract class ThemePresetMapper {

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected CommonIdMapper idMapper;

    public ThemePresetView toView(ThemePresetJpaEntity e) {
        JsonNode configNode = null;
        try {
            configNode = objectMapper.readTree(e.getConfig());
        } catch (Exception ex) {
            // ignore; return null config
        }
        return new ThemePresetView(
            idMapper.mapToThemePresetId(e.getId()),
            e.getCode(),
            e.getVendor(),
            configNode,
            e.getLabelKey(),
            e.isActive(),
            e.isDefaultPreset(),
            e.getCreatedAt(),
            e.getUpdatedAt()
        );
    }
}
