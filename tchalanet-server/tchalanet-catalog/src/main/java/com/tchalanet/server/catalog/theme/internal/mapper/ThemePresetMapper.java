package com.tchalanet.server.catalog.theme.internal.mapper;


import com.tchalanet.server.catalog.theme.api.ThemePresetView;
import com.tchalanet.server.catalog.theme.internal.persistence.ThemePresetJpaEntity;
import com.tchalanet.server.common.json.mapper.CommonIdMapper;
import com.tchalanet.server.common.util.JsonUtils;
import org.mapstruct.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;

@Mapper(componentModel = "spring", uses = {CommonIdMapper.class})
public abstract class ThemePresetMapper {

    @Autowired
    protected JsonUtils jsonUtils;

    @Autowired
    protected CommonIdMapper idMapper;

    public ThemePresetView toView(ThemePresetJpaEntity e) {
        JsonNode configNode = null;
        try {
            configNode = jsonUtils.readValue(e.getConfig(), JsonNode.class);
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
