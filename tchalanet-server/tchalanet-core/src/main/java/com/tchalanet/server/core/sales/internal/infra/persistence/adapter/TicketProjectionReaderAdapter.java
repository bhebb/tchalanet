package com.tchalanet.server.core.sales.internal.infra.persistence.adapter;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.OfflineSubmissionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageMapper;
import com.tchalanet.server.core.sales.api.model.print.TicketPrintView;
import com.tchalanet.server.core.sales.api.model.status.TicketSaleStatus;
import com.tchalanet.server.core.sales.api.model.view.TicketDetailsView;
import com.tchalanet.server.core.sales.api.model.view.TicketForDrawSettlementView;
import com.tchalanet.server.core.sales.api.model.view.TicketForPayoutView;
import com.tchalanet.server.core.sales.api.model.view.TicketRow;
import com.tchalanet.server.core.sales.api.query.ListTicketsQuery;
import com.tchalanet.server.core.sales.internal.application.port.out.TicketProjectionReaderPort;
import com.tchalanet.server.core.sales.internal.infra.persistence.entity.TicketJpaEntity;
import com.tchalanet.server.core.sales.internal.infra.persistence.repository.TicketJpaRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TicketProjectionReaderAdapter implements TicketProjectionReaderPort {
    private static final String SORT_CREATED_AT = "createdAt";
    private static final String SORT_TOTAL_AMOUNT = "totalAmount";
    private static final String SORT_TICKET_CODE = "ticketCode";

    private final TicketJpaRepository repository;

    @Override
    public TicketDetailsView getDetailsById(TicketId ticketId) {
        var entity = repository.findById(ticketId.value())
            .orElseThrow(() -> ProblemRest.notFound("ticket.not_found", ticketId));
        return toDetailsView(entity);
    }

    @Override
    public TicketPrintView getPrintViewById(TicketId ticketId) {
        throw new UnsupportedOperationException("Use TicketPrintReaderPort for print projections");
    }

    @Override
    public TicketForPayoutView getForPayoutById(TicketId ticketId) {
        var entity = repository.findById(ticketId.value())
            .orElseThrow(() -> ProblemRest.notFound("ticket.not_found", ticketId));
        return toPayoutView(entity);
    }

    @Override
    public List<TicketForDrawSettlementView> findForDrawSettlement(DrawId drawId) {
        var spec = (Specification<TicketJpaEntity>) (root, ignoredQuery, cb) -> cb.equal(root.get("drawId"), drawId.value());
        return repository.findAll(spec).stream()
            .map(this::toDrawSettlementView)
            .toList();
    }

    @Override
    public TchPage<TicketRow> list(ListTicketsQuery query) {
        var status = parseStatus(query.status());
        var spec = byFilters(query, status);
        var page = repository.findAll(spec, toSafePageable(query.page().pageable()));
        return TchPageMapper.map(page, this::toRow);
    }

    @Override
    public Optional<TicketPrintView> findPrintView(TicketId ticketId) {
        return Optional.empty();
    }

    private Specification<TicketJpaEntity> byFilters(ListTicketsQuery query, TicketSaleStatus status) {
        return (root, ignoredQuery, cb) -> {
            var predicates = new java.util.ArrayList<Predicate>();

            if (query.terminalId() != null) {
                predicates.add(cb.equal(root.get("terminalId"), query.terminalId().value()));
            }
            if (query.outletId() != null) {
                predicates.add(cb.equal(root.get("outletId"), query.outletId().value()));
            }
            if (query.drawId() != null) {
                predicates.add(cb.equal(root.get("drawId"), query.drawId().value()));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("saleStatus"), status));
            }
            if (query.from() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), query.from()));
            }
            if (query.to() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), query.to()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private Pageable toSafePageable(Pageable pageable) {
        var mappedSort = mapSort(pageable.getSort());
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), mappedSort);
    }

    private Sort mapSort(Sort sort) {
        if (sort == null || sort.isUnsorted()) {
            return Sort.by(Sort.Order.desc(SORT_CREATED_AT));
        }

        var mappedOrders = sort.stream()
            .map(order -> new Sort.Order(order.getDirection(), mapProperty(order.getProperty())))
            .toList();
        return Sort.by(mappedOrders);
    }

    private String mapProperty(String property) {
        return switch (property) {
            case SORT_CREATED_AT -> "createdAt";
            case SORT_TOTAL_AMOUNT -> "totalAmount";
            case SORT_TICKET_CODE -> "ticketCode";
            default -> throw ProblemRest.badRequest("ticket.filter.invalid_sort");
        };
    }

    private TicketSaleStatus parseStatus(String rawStatus) {
        if (rawStatus == null || rawStatus.isBlank()) {
            return null;
        }
        try {
            return TicketSaleStatus.valueOf(rawStatus.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw ProblemRest.badRequest("ticket.filter.invalid_status");
        }
    }

    private TicketDetailsView toDetailsView(TicketJpaEntity entity) {
        return new TicketDetailsView(
            TicketId.of(entity.getId()),
            TenantId.of(entity.getTenantId()),
            entity.getTicketCode(),
            entity.getSaleStatus(),
            DrawId.of(entity.getDrawId()),
            com.tchalanet.server.common.types.id.SalesSessionId.of(entity.getSalesSessionId()),
            com.tchalanet.server.common.types.id.OutletId.of(entity.getOutletId()),
            com.tchalanet.server.common.types.id.TerminalId.of(entity.getTerminalId()),
            com.tchalanet.server.common.types.id.UserId.of(entity.getSellerUserId()),
            entity.getOfflineSubmissionId() == null ? null : OfflineSubmissionId.of(entity.getOfflineSubmissionId()),
            cents(entity.getTotalAmount()),
            entity.getCurrency(),
            entity.getPlacedAt(),
            entity.getCancelledAt()
        );
    }

    private TicketRow toRow(TicketJpaEntity entity) {
        return new TicketRow(
            TicketId.of(entity.getId()),
            entity.getTicketCode(),
            entity.getPublicCode(),
            entity.getSaleStatus(),
            DrawId.of(entity.getDrawId()),
            entity.getDrawChannelName(),
            entity.getDrawScheduledAt(),
            cents(entity.getTotalAmount()),
            entity.getCurrency(),
            entity.getPlacedAt()
        );
    }

    private TicketForPayoutView toPayoutView(TicketJpaEntity entity) {
        return new TicketForPayoutView(
            TicketId.of(entity.getId()),
            TenantId.of(entity.getTenantId()),
            entity.getTicketCode(),
            entity.getSaleStatus(),
            DrawId.of(entity.getDrawId()),
            cents(entity.getWinningAmount()),
            entity.getCurrency()
        );
    }

    private TicketForDrawSettlementView toDrawSettlementView(TicketJpaEntity entity) {
        return new TicketForDrawSettlementView(
            TicketId.of(entity.getId()),
            DrawId.of(entity.getDrawId()),
            entity.getSaleStatus(),
            cents(entity.getTotalAmount()),
            entity.getCurrency()
        );
    }

    private long cents(BigDecimal amount) {
        if (amount == null) {
            return 0L;
        }
        return amount.movePointRight(2).longValue();
    }
}
