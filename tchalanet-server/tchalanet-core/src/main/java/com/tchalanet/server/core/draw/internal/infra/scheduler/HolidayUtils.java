package com.tchalanet.server.core.draw.internal.infra.scheduler;

import com.tchalanet.server.core.uslottery.internal.infra.config.UsLotteryProperties;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HolidayUtils {

  private static final DateTimeFormatter MM_DD = DateTimeFormatter.ofPattern("MM-dd");

  private final UsLotteryProperties usLotteryProperties;
  private final Map<String, Set<LocalDate>> holidayCache = new ConcurrentHashMap<>();
  private final Logger log = LoggerFactory.getLogger(HolidayUtils.class);

  /**
   * Check if the given local date is a holiday for the provider inferred from the channel code.
   * Returns false if no provider could be inferred or no holidays configured.
   */
  public boolean isHolidayForChannel(String channelCode, LocalDate date) {
    if (channelCode == null || date == null) return false;
    String provider = inferProviderKey(channelCode);
    return isHoliday(provider, date);
  }

  /**
   * Vérifie si la date est listée dans les holidays en format MM-dd au niveau commun ou provider.
   * Protège contre les nulls et utilisera les propriétés chargées via UsLotteryProperties.
   */
  public boolean isHoliday(String providerKey, LocalDate date) {
    if (date == null) return false;

    String mmdd = date.format(MM_DD);

    // common holidays
    if (usLotteryProperties.getCommon() != null
        && usLotteryProperties.getCommon().getHolidays() != null
        && usLotteryProperties.getCommon().getHolidays().contains(mmdd)) {
      return true;
    }

    // provider-specific holidays
    if (providerKey == null) return false;
    var providers = usLotteryProperties.getProviders();
    if (providers == null) return false;
    var provider = providers.get(providerKey.toLowerCase());
    return provider != null
        && provider.getHolidays() != null
        && provider.getHolidays().contains(mmdd);
  }

  private String inferProviderKey(String channelCode) {
    String c = channelCode == null ? "" : channelCode.toLowerCase();
    if (c.contains("us_ny") || c.contains("ny_") || c.contains("ny")) return "ny";
    if (c.contains("us_fl") || c.contains("fl_") || c.contains("fl") || c.contains("florida"))
      return "fl";
    return null;
  }
}
