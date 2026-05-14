package com.tchalanet.server.catalog.drawchannel.api.model;

/** Source d'un tirage: interne système, US Lottery, ou saisie manuelle. */
public enum DrawSource {
  SYSTEM,
  AUTO,
  EXTERNAL,
  US_LOTTERY,
  NY_OPEN_DATA,
  FL_APIM,
  MANUAL,
  ADMIN_OVERRIDE
}
