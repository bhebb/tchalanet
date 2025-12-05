package com.tchalanet.server.features.notifications.list_my_notifications;

import com.tchalanet.server.features.notifications.shared.NotificationDto;
import com.tchalanet.server.features.notifications.shared.NotificationJpaRepository;
import com.tchalanet.server.features.notifications.shared.NotificationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

/**
 * Use case listant les notifications pour l'utilisateur connecté.
 */
@Service
@RequiredArgsConstructor
public class ListMyNotificationsService {

    private final NotificationJpaRepository repository;


    public Page<NotificationDto> getNotifications(ListMyNotificationsQuery query) {
        var pageable = PageRequest.of(query.page(), query.size());

        var page = query.unreadOnly()
            ? repository.findByTenantIdAndUserIdAndReadIsFalseOrderByCreatedAtDesc(
            query.tenantId(), query.userId(), pageable)
            : repository.findByTenantIdAndUserIdOrderByCreatedAtDesc(
            query.tenantId(), query.userId(), pageable);

        return page.map(NotificationMapper::toDto);

    }
}
