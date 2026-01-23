package com.tchalanet.server.catalog.theme.internal.mapper;

import com.tchalanet.server.catalog.theme.api.ThemePresetView;
import com.tchalanet.server.catalog.theme.internal.persistence.ThemePresetJpaEntity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring")
public abstract class ThemePresetMapper {

    public static final ThemePresetMapper INSTANCE = Mappers.getMapper(ThemePresetMapper.class);

    @Autowired
    protected ObjectMapper objectMapper;

    public ThemePresetView toView(ThemePresetJpaEntity e) {
        JsonNode configNode = null;
        try {
            configNode = objectMapper.readTree(e.getConfig());
        } catch (Exception ex) {
            // ignore; return null config
        }
        return new ThemePresetView(
            e.getId(),
            e.getCode(),
            e.getVendor(),
            configNode,
            e.getLabelKey(),
            e.isActive(),
            e.getCreatedAt(),
            e.getUpdatedAt()
        );
    }
}
