package com.tchalanet.server.core.draw.domain.model;

/**
 * Statut d'un tirage (cycle de vie)
 *
 * <p>SCHEDULED -> OPEN -> CLOSED -> RESULTED -> SETTLED CANCELED peut intervenir depuis
 * SCHEDULED|OPEN|CLOSED
 */
public enum DrawStatus {
  SCHEDULED,
  OPEN,
  CLOSED,
  RESULTED,
  SETTLED,
  ARCHIVED,
  CANCELED;

    public boolean isActive() {
        return this == SCHEDULED || this == OPEN;
    }
}
