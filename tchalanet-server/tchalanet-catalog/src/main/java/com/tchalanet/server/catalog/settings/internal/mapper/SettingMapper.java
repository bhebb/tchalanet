package com.tchalanet.server.catalog.settings.internal.mapper;

import com.tchalanet.server.catalog.settings.api.model.ResolvedSettingView;
import com.tchalanet.server.catalog.settings.api.model.SettingLevel;
import com.tchalanet.server.catalog.settings.api.model.SettingView;
import com.tchalanet.server.catalog.settings.internal.persistence.SettingEntity;
import com.tchalanet.server.common.json.mapper.CommonIdMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * Setting Mapper (INTERNAL)
 *
 * <p>Maps between JPA entities and public API views. MUST NOT be exposed outside the catalog
 * module.
 *
 * <p>Uses {@link CommonIdMapper} for ID conversions (UUID ↔ typed IDs).
 */
@Mapper(componentModel = "spring", uses = {CommonIdMapper.class})
public interface SettingMapper {

  /**
   * Entity → View (admin view)
   *
   * @param entity JPA entity
   * @return admin view
   */
  SettingView toView(SettingEntity entity);

  /**
   * Entities → Views (admin views)
   *
   * @param entities JPA entities
   * @return admin views
   */
  List<SettingView> toViews(List<SettingEntity> entities);

  /**
   * Entity → ResolvedView (for resolution)
   *
   * @param entity JPA entity
   * @param effectiveLevel which level provided this value
   * @return resolved view
   */
  default ResolvedSettingView toResolvedView(SettingEntity entity, SettingLevel effectiveLevel) {
    return new ResolvedSettingView(
        entity.getNamespace(),
        entity.getSettingKey(),
        entity.getValueType(),
        entity.getSettingValue(),
        effectiveLevel);
  }
}
