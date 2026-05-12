package com.tchalanet.server.catalog.settings.api.model;

/**
 * Setting Value Type
 *
 * <p>Defines the data type for setting values. All values are stored as text but are validated and
 * parsed according to their declared type.
 */
public enum SettingValueType {
  /** String value (no parsing) */
  STRING,

  /** Integer value (32-bit) */
  INT,

  /** Long integer value (64-bit) */
  LONG,

  /** Decimal value (BigDecimal) */
  DECIMAL,

  /** Boolean value (true/false) */
  BOOLEAN,

  /** JSON value (object, array, or primitive) */
  JSON
}
