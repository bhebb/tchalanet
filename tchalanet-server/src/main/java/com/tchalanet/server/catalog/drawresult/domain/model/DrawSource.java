package com.tchalanet.server.catalog.drawresult.domain.model;

/** Source d'un tirage: interne système, US Lottery, ou saisie manuelle. */
public enum DrawSource {
  SYSTEM,
  EXTERNAL,
  US_LOTTERY,
  NY_OPEN_DATA,
  FL_APIM,
  MANUAL,
  ADMIN_OVERRIDE
}
