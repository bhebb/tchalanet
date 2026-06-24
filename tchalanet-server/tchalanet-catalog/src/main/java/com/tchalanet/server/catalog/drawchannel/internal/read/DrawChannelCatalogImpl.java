package com.tchalanet.server.catalog.drawchannel.internal.read;

import tools.jackson.databind.JsonNode;
import com.tchalanet.server.catalog.drawchannel.api.DrawChannelCatalog;
import com.tchalanet.server.catalog.drawchannel.api.model.*;
import com.tchalanet.server.catalog.drawchannel.internal.cache.DrawChannelCacheNames;
import com.tchalanet.server.catalog.drawchannel.internal.mapper.DrawChannelGameMapper;
import com.tchalanet.server.catalog.drawchannel.internal.mapper.DrawChannelMapper;
import com.tchalanet.server.catalog.drawchannel.internal.persistence.DrawChannelEntity;
import com.tchalanet.server.catalog.drawchannel.internal.persistence.DrawChannelGameRepository;
import com.tchalanet.server.catalog.drawchannel.internal.persistence.DrawChannelRepository;
import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.ResultSlotId;
import com.tchalanet.server.common.types.id.TenantGameId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.json.utils.JsonUtils;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DrawChannelCatalogImpl implements DrawChannelCatalog {

  private final DrawChannelRepository repository;
  private final DrawChannelMapper mapper;
  private final DrawChannelGameRepository gameRepository;
  private final DrawChannelGameMapper gameMapper;
  private final JsonUtils jsonUtils;

  /**
   * RLS NOTE:
   * - tenant scoping is enforced by PostgreSQL policies using app.current_tenant.
   * - this method MUST NOT add tenant filters.
   * - soft-delete visibility MUST be controlled by app.deleted_visibility (RLS), not by code.
   */
  @Override
  @Cacheable(value = DrawChannelCacheNames.BY_TENANT, key = "#tenantId.value() + ':' + #activeOnly")
  public List<DrawChannelSummaryView> listAll(TenantId tenantId, Boolean activeOnly) {
    // Allowed functional filter: active flag (not a tenant filter)
    List<DrawChannelEntity> entities;
    if (Boolean.TRUE.equals(activeOnly)) {
      entities = repository.findByActiveTrueOrderBySortOrderAsc();
    } else {
      entities = repository.findAllByOrderBySortOrderAsc();
    }

    return entities.stream().map(this::toLightSummary).toList();
  }

  /**
   * IMPORTANT: tenantId param is for cache key only.
   * SQL must not filter by tenant; RLS enforces it.
   */
  @Override
  @Cacheable(value = DrawChannelCacheNames.BY_ID, key = "#tenantId.value() + ':' + #id.value()")
  public Optional<DrawChannelView> findById(TenantId tenantId, DrawChannelId id) {
    return repository.findById(id.value()).map(mapper::toView);
  }

  @Override
  @Cacheable(value = DrawChannelCacheNames.BY_TENANT, key = "#tenantId.value() + ':code:' + #code")
  public Optional<DrawChannelView> findByTenantAndCode(TenantId tenantId, String code) {
    if (code == null || code.isBlank()) return Optional.empty();
    return repository.findFirstByCodeIgnoreCase(code.trim()).map(mapper::toView);
  }

  @Override
  public List<DrawChannelGameView> listGamesByChannel(TenantId tenantId, DrawChannelId channelId) {
    // No tenant filter; RLS scopes draw_channel_game (or via join policy) to current tenant.
    var games = gameRepository.findByDrawChannelId(channelId.value());
    return gameMapper.toViews(games);
  }

  @Override
  @Cacheable(value = DrawChannelCacheNames.BY_TENANT_GAME_MAP, key = "#tenantId.value()")
  public List<ChannelGamesView> listChannelGames(TenantId tenantId) {
    List<Object[]> rows = gameRepository.findChannelCodeAndGameRows();
    Map<String, List<GameSummaryView>> map = new HashMap<>();

    for (Object[] r : rows) {
      String code = (String) r[0];
      String gameCode = (String) r[1];
      UUID tenantGameUuid = (UUID) r[2];
      Boolean enabled = (r[3] == null) ? Boolean.FALSE : (Boolean) r[3];

      Object rawFlags = r[4];
      JsonNode flags;
      if (rawFlags == null) flags = null;
      else if (rawFlags instanceof JsonNode jn) flags = jn;
      else flags = jsonUtils.parse(rawFlags.toString());

      map.computeIfAbsent(code, k -> new ArrayList<>())
          .add(new GameSummaryView(TenantGameId.nullableOf(tenantGameUuid), gameCode, enabled, flags));
    }

    List<ChannelGamesView> result = new ArrayList<>();
    for (var e : map.entrySet()) {
      result.add(new ChannelGamesView(e.getKey(), e.getValue()));
    }
    return result;
  }

  @Override
  @Cacheable(value = DrawChannelCacheNames.CALENDAR_ROWS,
      key = "#tenantId.value() + ':' + #activeOnly + ':' + #enabledOnly")
  public List<DrawChannelCalendarRow> listCalendarRows(TenantId tenantId, Boolean activeOnly, Boolean enabledOnly) {
    List<Object[]> rows = gameRepository.findCalendarRows();
    List<DrawChannelCalendarRow> result = new ArrayList<>();

    for (Object[] r : rows) {
      String code         = (String) r[0];
      // r[1] = game_code (not used in row)
      UUID tenantGameUuid = (UUID) r[2];
      // r[3] = tg.id (same as tenant_game_id, redundant)
      UUID channelUuid    = (UUID) r[4];
      UUID resultSlotUuid = (UUID) r[5];
      String timezone     = (String) r[6];
      LocalTime drawTime  = r[7] == null ? null : (r[7] instanceof java.sql.Time t ? t.toLocalTime() : LocalTime.parse(r[7].toString()));
      LocalTime salesOpen = r[8] == null ? null : (r[8] instanceof java.sql.Time t ? t.toLocalTime() : LocalTime.parse(r[8].toString()));
      int cutoffSec       = r[9] == null ? 0 : ((Number) r[9]).intValue();
      String daysOfWeek   = (String) r[10];
      String defaultSrc   = (String) r[11];
      boolean channelActive = r[12] != null && (Boolean) r[12];
      boolean dcgEnabled    = r[13] != null && (Boolean) r[13];
      int sortOrder         = r[14] == null ? 0 : ((Number) r[14]).intValue();
      UUID dependsOn        = (UUID) r[15];

      if (Boolean.TRUE.equals(activeOnly) && !channelActive) continue;
      if (Boolean.TRUE.equals(enabledOnly) && !dcgEnabled) continue;

      result.add(new DrawChannelCalendarRow(
          DrawChannelId.of(channelUuid),
          TenantGameId.of(tenantGameUuid),
          code,
          timezone,
          drawTime,
          salesOpen,
          cutoffSec,
          daysOfWeek,
          ResultSlotId.of(resultSlotUuid),
          defaultSrc,
          channelActive,
          dcgEnabled,
          sortOrder,
          dependsOn == null ? null : DrawChannelId.of(dependsOn)
      ));
    }
    return result;
  }

  @Override
  public TchPage<DrawChannelView> search(DrawChannelSearchCriteria criteria, TchPageRequest pageReq) {
    Specification<DrawChannelEntity> spec = buildSpecification(criteria);

    PageRequest springPageRequest = PageRequest.of(
        pageReq.pageable().getPageNumber(),
        pageReq.pageable().getPageSize(),
        pageReq.pageable().getSort()
    );

    Page<DrawChannelEntity> page = repository.findAll(spec, springPageRequest);
    List<DrawChannelView> items = page.getContent().stream().map(mapper::toView).toList();

    return TchPage.of(
        items,
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages(),
        page.isLast(),
        page.hasNext(),
        page.hasPrevious()
    );
  }

  private Specification<DrawChannelEntity> buildSpecification(DrawChannelSearchCriteria criteria) {
    Specification<DrawChannelEntity> spec = (root, query, cb) -> cb.conjunction();

    // 🚫 NO tenant predicate here (RLS does it)
    // Allowed functional filters:
    if (criteria.active() != null) {
      spec = spec.and((root, query, cb) -> cb.equal(root.get("active"), criteria.active()));
    }

    if (criteria.code() != null && !criteria.code().isBlank()) {
      spec = spec.and((root, query, cb) ->
          cb.equal(cb.lower(root.get("code")), criteria.code().trim().toLowerCase()));
    }

    if (criteria.nameContains() != null && !criteria.nameContains().isBlank()) {
      spec = spec.and((root, query, cb) ->
          cb.like(cb.lower(root.get("name")), "%" + criteria.nameContains().toLowerCase() + "%"));
    }

    if (criteria.resultSlotId() != null) {
      spec = spec.and((root, query, cb) -> cb.equal(root.get("resultSlotId"), criteria.resultSlotId().value()));
    }

    if (criteria.externalProvider() != null && !criteria.externalProvider().isBlank()) {
      spec = spec.and((root, query, cb) ->
          cb.equal(cb.lower(root.get("externalProvider")), criteria.externalProvider().trim().toLowerCase()));
    }

    return spec;
  }

  @Override
  public List<DrawChannelView> listAllFull(TenantId tenantId) {
    return repository.findAllByOrderBySortOrderAsc()
        .stream()
        .map(mapper::toView)
        .toList();
  }

  private DrawChannelSummaryView toLightSummary(DrawChannelEntity e) {
    ZoneId zone;
    try {
      zone = e.getTimezone() == null ? null : ZoneId.of(e.getTimezone());
    } catch (Exception ex) {
      zone = null;
    }

    LocalTime drawTime = e.getDrawTime();
    int cutoffSec = e.getCutoffSec();
    LocalTime cutoffTime = (drawTime == null) ? null : drawTime.minusSeconds(Math.max(0, cutoffSec));

    return new DrawChannelSummaryView(
        e.getCode(),
        e.getName(),
        drawTime,
        cutoffTime,
        zone,
        e.isActive()
    );
  }

}
