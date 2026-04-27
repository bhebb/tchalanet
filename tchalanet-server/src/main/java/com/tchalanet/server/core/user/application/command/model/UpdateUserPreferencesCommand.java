package com.tchalanet.server.core.user.application.command.model;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.types.enums.ThemeMode;
import java.time.ZoneId;
import java.util.Currency;
import java.util.Locale;
import java.util.Optional;

public record UpdateUserPreferencesCommand(
    UserId userId,
    Optional<ThemeMode> prefThemeMode,
    Optional<Short> prefDensity,
    Optional<Locale> prefLocale,
    Optional<ZoneId> prefTimeZone,
    Optional<Currency> prefCurrency
) implements Command<Void> {}
