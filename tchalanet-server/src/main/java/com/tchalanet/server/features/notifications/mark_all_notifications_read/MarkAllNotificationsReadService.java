package com.tchalanet.server.features.notifications.mark_all_notifications_read;

import com.tchalanet.server.features.notifications.shared.NotificationEntity;
import com.tchalanet.server.features.notifications.shared.NotificationJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * (Optionnel) Use case pour marquer toutes les notifications de l'utilisateur comme lues.
 */
@Service
@RequiredArgsConstructor
public class MarkAllNotificationsReadService {

    private final NotificationJpaRepository repo;


    public void handle(MarkAllNotificationsReadCommand command) {
        repo.markAllRead(command.tenantId(), command.userId(), Instant.now());
    }
}

