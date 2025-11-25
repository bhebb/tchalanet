package com.tchalanet.server.game.web.dto;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GameDto {
  private UUID id;
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
