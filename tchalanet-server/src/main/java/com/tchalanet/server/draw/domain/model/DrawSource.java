package com.tchalanet.server.draw.domain.model;

/** Source d'un tirage: interne système, US Lottery, ou saisie manuelle. */
public enum DrawSource {
  SYSTEM,
  EXTERNAL,
  US_LOTTERY,
  MANUAL,
  ADMIN_OVERRIDE
}
