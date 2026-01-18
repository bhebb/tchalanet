package com.tchalanet.server.catalog.game.domain.model;

import com.tchalanet.server.common.types.id.GameId;
import java.time.Instant;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Game {
  private GameId id;
  private String code;
  private String name;
  private String category;
  private Integer minDigits;
  private Integer maxDigits;
  private String combination;
  private String description;
  private Boolean active;
  private Integer sortOrder;
  private Instant createdAt;
  private Instant updatedAt;
}
