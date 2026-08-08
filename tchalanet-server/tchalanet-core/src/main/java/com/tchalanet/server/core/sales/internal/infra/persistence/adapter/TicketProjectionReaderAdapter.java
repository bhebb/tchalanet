package com.tchalanet.server.core.sales.internal.infra.persistence.adapter;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageMapper;
import com.tchalanet.server.core.sales.api.error.SalesErrorCodes;
import com.tchalanet.server.core.sales.api.model.print.TicketPrintView;
import com.tchalanet.server.core.sales.api.model.status.TicketResultStatus;
import com.tchalanet.server.core.sales.api.model.status.TicketSaleStatus;
import com.tchalanet.server.core.sales.api.model.status.TicketSettlementStatus;
import com.tchalanet.server.core.sales.api.model.view.TicketDetailsView;
import com.tchalanet.server.core.sales.api.model.view.TicketForDrawSettlementView;
import com.tchalanet.server.core.sales.api.model.view.TicketForPayoutView;
import com.tchalanet.server.core.sales.api.model.view.TicketRow;
import com.tchalanet.server.core.sales.api.query.ListTicketsQuery;
import com.tchalanet.server.core.sales.internal.application.port.out.TicketProjectionReaderPort;
import com.tchalanet.server.core.sales.internal.infra.persistence.entity.TicketJpaEntity;
import com.tchalanet.server.core.sales.internal.infra.persistence.repository.TicketJpaRepository;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
    var entity =
        repository
            .findById(ticketId.value())
            .orElseThrow(() -> ProblemRest.of(SalesErrorCodes.TICKET_NOT_FOUND));
    return toDetailsView(entity);
  }

  @Override
  public TicketPrintView getPrintViewById(TicketId ticketId) {
    throw new UnsupportedOperationException("Use TicketPrintReaderPort for print projections");
  }

  @Override
  public TicketForPayoutView getForPayoutById(TicketId ticketId) {
    var entity =
        repository
            .findById(ticketId.value())
            .orElseThrow(() -> ProblemRest.of(SalesErrorCodes.TICKET_NOT_FOUND));
    return toPayoutView(entity);
  }

  @Override
  public List<TicketForDrawSettlementView> findForDrawSettlement(DrawId drawId) {
    var spec =
        (Specification<TicketJpaEntity>)
            (root, ignoredQuery, cb) -> cb.equal(root.get("drawId"), drawId.value());
    return repository.findAll(spec).stream().map(this::toDrawSettlementView).toList();
  }

  @Override
  public TchPage<TicketRow> list(ListTicketsQuery query) {
    var status = parseStatus(query.status());
    var resultStatus = parseResultStatus(query.resultStatus());
    var settlementStatus = parseSettlementStatus(query.settlementStatus());
    var spec = byFilters(query, status, resultStatus, settlementStatus);
    var page = repository.findAll(spec, toSafePageable(query.page().pageable()));
    return TchPageMapper.map(page, this::toRow);
  }

  @Override
  public Optional<TicketPrintView> findPrintView(TicketId ticketId) {
    return Optional.empty();
  }

  private Specification<TicketJpaEntity> byFilters(
      ListTicketsQuery query,
      TicketSaleStatus status,
      TicketResultStatus resultStatus,
      TicketSettlementStatus settlementStatus) {
    return (root, ignoredQuery, cb) -> {
      var predicates = new java.util.ArrayList<Predicate>();

      if (query.sellerTerminalId() != null) {
        predicates.add(cb.equal(root.get("sellerTerminalId"), query.sellerTerminalId().value()));
      }
      if (query.drawId() != null) {
        predicates.add(cb.equal(root.get("drawId"), query.drawId().value()));
      }
      if (status != null) {
        predicates.add(cb.equal(root.get("saleStatus"), status));
      }
      if (resultStatus != null) {
        predicates.add(cb.equal(root.get("resultStatus"), resultStatus));
      }
      if (settlementStatus != null) {
        predicates.add(cb.equal(root.get("settlementStatus"), settlementStatus));
      }
      var provider = normalizeProvider(query.provider());
      if (provider != null) {
        predicates.add(cb.equal(cb.upper(root.<String>get("resultProvider")), provider));
      }
      var slotKey = normalizeSlotKey(query.slotKey());
      if (slotKey != null) {
        predicates.add(cb.equal(cb.upper(root.<String>get("resultSlotKey")), slotKey));
      }
      if (Boolean.TRUE.equals(query.winningOnly())) {
        predicates.add(cb.equal(root.get("resultStatus"), TicketResultStatus.WON));
        predicates.add(cb.greaterThan(root.<BigDecimal>get("winningAmount"), BigDecimal.ZERO));
      }
      var codeQuery = codeQuery(query.q());
      if (codeQuery != null) {
        var rawPattern = "%" + escapeLike(codeQuery.raw()) + "%";
        var compactPattern = "%" + escapeLike(codeQuery.compact()) + "%";
        var compactTicketCode =
            cb.lower(
                cb.function(
                    "replace",
                    String.class,
                    root.get("ticketCode"),
                    cb.literal("-"),
                    cb.literal("")));
        var compactPublicCode =
            cb.lower(
                cb.function(
                    "replace",
                    String.class,
                    root.get("publicCode"),
                    cb.literal("-"),
                    cb.literal("")));
        predicates.add(
            cb.or(
                cb.like(cb.lower(root.get("ticketCode")), rawPattern, '\\'),
                cb.like(cb.lower(root.get("publicCode")), rawPattern, '\\'),
                cb.like(compactTicketCode, compactPattern, '\\'),
                cb.like(compactPublicCode, compactPattern, '\\')));
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

  private static String normalizeProvider(String provider) {
    if (provider == null || provider.isBlank()) {
      return null;
    }
    return provider.trim().toUpperCase(java.util.Locale.ROOT);
  }

  private static String normalizeSlotKey(String slotKey) {
    if (slotKey == null || slotKey.isBlank()) {
      return null;
    }
    return slotKey.trim().toUpperCase(java.util.Locale.ROOT);
  }

  private Pageable toSafePageable(Pageable pageable) {
    var mappedSort = mapSort(pageable.getSort());
    return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), mappedSort);
  }

  private Sort mapSort(Sort sort) {
    if (sort == null || sort.isUnsorted()) {
      return Sort.by(Sort.Order.desc(SORT_CREATED_AT));
    }

    var mappedOrders =
        sort.stream()
            .map(order -> new Sort.Order(order.getDirection(), mapProperty(order.getProperty())))
            .toList();
    return Sort.by(mappedOrders);
  }

  private String mapProperty(String property) {
    return switch (property) {
      case SORT_CREATED_AT -> "createdAt";
      case SORT_TOTAL_AMOUNT -> "totalAmount";
      case SORT_TICKET_CODE -> "ticketCode";
      default -> throw ProblemRest.of(SalesErrorCodes.TICKET_FILTER_INVALID_SORT);
    };
  }

  private CodeQuery codeQuery(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    var normalized = raw.trim().toLowerCase(Locale.ROOT);
    return new CodeQuery(normalized, normalized.replace("-", ""));
  }

  private String escapeLike(String value) {
    return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }

  private record CodeQuery(String raw, String compact) {}

  private TicketSaleStatus parseStatus(String rawStatus) {
    return parseEnum(TicketSaleStatus.class, rawStatus);
  }

  private TicketResultStatus parseResultStatus(String rawStatus) {
    return parseEnum(TicketResultStatus.class, rawStatus);
  }

  private TicketSettlementStatus parseSettlementStatus(String rawStatus) {
    return parseEnum(TicketSettlementStatus.class, rawStatus);
  }

  private <E extends Enum<E>> E parseEnum(Class<E> type, String rawStatus) {
    if (rawStatus == null || rawStatus.isBlank()) {
      return null;
    }
    try {
      return Enum.valueOf(type, rawStatus.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException ex) {
      throw ProblemRest.of(SalesErrorCodes.TICKET_FILTER_INVALID_STATUS, Map.of(), ex);
    }
  }

  private TicketDetailsView toDetailsView(TicketJpaEntity entity) {
    return new TicketDetailsView(
        TicketId.of(entity.getId()),
        TenantId.of(entity.getTenantId()),
        entity.getTicketCode(),
        entity.getSaleStatus(),
        entity.getResultStatus(),
        entity.getSettlementStatus(),
        DrawId.of(entity.getDrawId()),
        SellerTerminalId.nullableOf(entity.getSellerTerminalId()),
        cents(entity.getTotalAmount()),
        cents(entity.getWinningAmount()),
        cents(entity.getPaidAmount()),
        entity.getCurrency(),
        entity.getPlacedAt(),
        entity.getCancelledAt());
  }

  private TicketRow toRow(TicketJpaEntity entity) {
    return new TicketRow(
        TicketId.of(entity.getId()),
        entity.getTicketCode(),
        entity.getPublicCode(),
        entity.getSaleStatus(),
        entity.getResultStatus(),
        entity.getSettlementStatus(),
        DrawId.of(entity.getDrawId()),
        SellerTerminalId.nullableOf(entity.getSellerTerminalId()),
        entity.getDrawChannelCode(),
        entity.getResultSlotKey(),
        entity.getResultProvider(),
        entity.getResultTimezone(),
        entity.getDrawChannelName(),
        entity.getDrawScheduledAt(),
        cents(entity.getTotalAmount()),
        cents(entity.getWinningAmount()),
        cents(entity.getPaidAmount()),
        entity.getCurrency(),
        entity.getPlacedAt());
  }

  private TicketForPayoutView toPayoutView(TicketJpaEntity entity) {
    return new TicketForPayoutView(
        TicketId.of(entity.getId()),
        TenantId.of(entity.getTenantId()),
        entity.getTicketCode(),
        entity.getSaleStatus(),
        DrawId.of(entity.getDrawId()),
        cents(entity.getWinningAmount()),
        entity.getCurrency());
  }

  private TicketForDrawSettlementView toDrawSettlementView(TicketJpaEntity entity) {
    return new TicketForDrawSettlementView(
        TicketId.of(entity.getId()),
        DrawId.of(entity.getDrawId()),
        entity.getSaleStatus(),
        cents(entity.getTotalAmount()),
        entity.getCurrency());
  }

  private long cents(BigDecimal amount) {
    if (amount == null) {
      return 0L;
    }
    return amount.movePointRight(2).longValue();
  }
}
