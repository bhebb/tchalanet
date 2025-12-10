package com.tchalanet.server.core.sales.application.query.handler;

import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.sales.infra.persistence.TicketEntity;
import com.tchalanet.server.core.sales.infra.persistence.repository.SpringTicketJpaRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;

@UseCase
@RequiredArgsConstructor
public class ListRecentTicketsForCashierHandler {

    private final SpringTicketJpaRepository ticketRepository;

    public List<UUID> handle(UUID tenantId, List<UUID> sessionIds, int limit) {
        if (sessionIds == null || sessionIds.isEmpty()) return List.of();
        var page = ticketRepository.findByTenantIdAndSessionIdInOrderByCreatedAtDesc(tenantId, sessionIds, PageRequest.of(0, limit));
        return page.getContent().stream().map(TicketEntity::getId).toList();
    }
}

