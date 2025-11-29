package com.tchalanet.server.core.game.infra.persistence;

import com.tchalanet.server.core.game.domain.model.Game;

public final class GameMapper {
  public static Game toDomain(GameJpaEntity e) {
    if (e == null) return null;
    Game.GameId gid = e.getId() == null ? null : new Game.GameId(e.getId());
    return Game.builder()
        .id(gid)
        .code(e.getCode())
        .name(e.getName())
        .category(e.getCategory())
        .minDigits(e.getMinDigits())
        .maxDigits(e.getMaxDigits())
        .combination(e.getCombination())
        .description(e.getDescription())
        .active(e.getActive())
        .sortOrder(e.getSortOrder())
        .createdAt(e.getCreatedAt())
        .updatedAt(e.getUpdatedAt())
        .build();
  }

  public static GameJpaEntity toEntity(Game d) {
    if (d == null) return null;
    GameJpaEntity e = new GameJpaEntity();
    if (d.getId() != null && d.getId().value() != null) e.setId(d.getId().value());
    e.setCode(d.getCode());
    e.setName(d.getName());
    e.setCategory(d.getCategory());
    e.setMinDigits(d.getMinDigits());
    e.setMaxDigits(d.getMaxDigits());
    e.setCombination(d.getCombination());
    e.setDescription(d.getDescription());
    e.setActive(d.getActive() == null ? Boolean.TRUE : d.getActive());
    e.setSortOrder(d.getSortOrder() == null ? 0 : d.getSortOrder());
    return e;
  }
}
