package com.tchalanet.server.game.infra.persistence;

import com.tchalanet.server.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;

@Entity
@Table(name = "game")
@Audited
@Getter
@Setter
@NoArgsConstructor
public class GameJpaEntity extends BaseEntity {

  @Column(name = "code", length = 32, nullable = false)
  private String code;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "category", nullable = false)
  private String category;

  @Column(name = "min_digits", nullable = false)
  private Integer minDigits;

  @Column(name = "max_digits", nullable = false)
  private Integer maxDigits;

  @Column(name = "combination", nullable = false)
  private String combination;

  @Column(name = "description")
  private String description;

  @Column(name = "active", nullable = false)
  private Boolean active = Boolean.TRUE;

  @Column(name = "sort_order", nullable = false)
  private Integer sortOrder = 0;
}
