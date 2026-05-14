package com.tchalanet.server.catalog.i18n.internal.mapper;

import com.tchalanet.server.catalog.i18n.api.model.I18nOverrideView;
import com.tchalanet.server.catalog.i18n.internal.persistence.I18nOverrideEntity;
import com.tchalanet.server.common.mapper.CommonIdMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * I18n Override Mapper (INTERNAL)
 *
 * <p>Maps between JPA entities and public API views. MUST NOT be exposed outside the catalog
 * module.
 *
 * <p>Uses {@link CommonIdMapper} for ID conversions (UUID ↔ typed IDs).
 */
@Mapper(componentModel = "spring", uses = {CommonIdMapper.class})
public interface I18nOverrideMapper {

  /**
   * Entity → View
   *
   * @param entity JPA entity
   * @return view
   */
  I18nOverrideView toView(I18nOverrideEntity entity);

  /**
   * Entities → Views
   *
   * @param entities JPA entities
   * @return views
   */
  List<I18nOverrideView> toViews(List<I18nOverrideEntity> entities);
}
