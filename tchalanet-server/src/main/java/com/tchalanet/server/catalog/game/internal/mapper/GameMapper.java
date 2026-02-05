package com.tchalanet.server.catalog.game.internal.mapper;

import com.tchalanet.server.catalog.game.api.GameSummaryView;
import com.tchalanet.server.catalog.game.api.GameView;
import com.tchalanet.server.catalog.game.internal.infra.persistence.GameJpaEntity;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface GameMapper {

  GameView toView(GameJpaEntity e);

  List<GameView> toViews(List<GameJpaEntity> entities);

  GameSummaryView toSummaryView(GameJpaEntity e);

  List<GameSummaryView> toSummaryViews(List<GameJpaEntity> entities);
}
