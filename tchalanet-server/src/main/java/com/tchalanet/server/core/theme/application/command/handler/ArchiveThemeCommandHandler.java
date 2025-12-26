package com.tchalanet.server.core.theme.application.command.handler;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.theme.application.command.model.ArchiveThemeCommand;
import com.tchalanet.server.core.theme.application.port.out.ThemeReaderPort;
import com.tchalanet.server.core.theme.application.port.out.ThemeWriterPort;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ArchiveThemeCommandHandler implements VoidCommandHandler<ArchiveThemeCommand> {

  private final ThemeReaderPort themeReaderPort;
  private final ThemeWriterPort themeWriterPort;
  private final Clock clock;

  @Override
  public void handle(ArchiveThemeCommand command) {
    TenantId tenantId = command.tenantId();
    UUID themeId = command.themeId();

    var theme =
        themeReaderPort
            .findById(themeId)
            .orElseThrow(() -> new IllegalArgumentException("Theme not found: " + themeId));

    if (theme.tenantId() == null || !theme.tenantId().equals(tenantId)) {
      throw new AccessDeniedException("Forbidden: Theme does not belong to tenant");
    }

    var now = Instant.now(clock);
    var archived = theme.archive(now);

    themeWriterPort.save(archived);
  }
}
