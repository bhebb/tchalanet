package com.tchalanet.server.common.time;

import java.time.ZoneId;

/** Central default time zone constants used across the application. */
public final class DefaultTimeZone {

  public static final ZoneId AMERICA_NEW_YORK = ZoneId.of("America/New_York");

  private DefaultTimeZone() {}
}
