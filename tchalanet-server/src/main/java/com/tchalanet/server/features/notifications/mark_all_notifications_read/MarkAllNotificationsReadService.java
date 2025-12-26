package com.tchalanet.server.features.notifications.mark_all_notifications_read;

import com.tchalanet.server.features.notifications.shared.NotificationJpaRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** (Optionnel) Use case pour marquer toutes les notifications de l'utilisateur comme lues. */
@Service
@RequiredArgsConstructor
public class MarkAllNotificationsReadService {

  private final NotificationJpaRepository repo;

  public void handle(MarkAllNotificationsReadCommand command) {
    repo.markAllRead(command.tenantId().uuid(), command.userId().uuid(), Instant.now());
  }
}
