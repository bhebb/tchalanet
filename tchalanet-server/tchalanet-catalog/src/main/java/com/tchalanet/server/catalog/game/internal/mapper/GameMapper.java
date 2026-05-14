package com.tchalanet.server.catalog.game.internal.mapper;

import com.tchalanet.server.catalog.game.api.model.GameView;
import com.tchalanet.server.catalog.game.internal.persistence.GameJpaEntity;
import com.tchalanet.server.common.mapper.CommonIdMapper;
import org.mapstruct.Mapper;

/**
 * MapStruct mapper for Game (JPA entity <-> View).
 * Maps to spec requirement G4 (mapping boundaries).
 * Uses CommonIdMapper for GameId conversions.
 */
@Mapper(componentModel = "spring", uses = {CommonIdMapper.class})
public interface GameMapper {

  /**
   * Map JPA entity to immutable View.
   */
  GameView toView(GameJpaEntity entity);
}
