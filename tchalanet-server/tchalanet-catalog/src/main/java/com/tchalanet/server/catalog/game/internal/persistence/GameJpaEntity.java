package com.tchalanet.server.catalog.game.internal.persistence;

import com.tchalanet.server.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
    name = "game",
    uniqueConstraints = @UniqueConstraint(name = "uq_game_code", columnNames = "code"))
@Getter
@Setter
public class GameJpaEntity extends BaseEntity {

    @Column(name = "code", nullable = false, length = 32, unique = true)
    private String code;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "category", nullable = false, length = 32)
    private String category; // HAITI

    @Column(name = "combination", nullable = false, length = 32)
    private String combination;

    @Column(name = "min_digits", nullable = false)
    private int minDigits;

    @Column(name = "max_digits", nullable = false)
    private int maxDigits;

    @Column(name = "description")
    private String description;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;
}
