package com.tchalanet.server.catalog.drawchannel.internal.write;

import com.tchalanet.server.catalog.drawchannel.api.model.DrawChannelView;
import com.tchalanet.server.catalog.drawchannel.internal.cache.DrawChannelCacheNames;
import com.tchalanet.server.catalog.drawchannel.internal.mapper.DrawChannelMapper;
import com.tchalanet.server.catalog.drawchannel.internal.persistence.DrawChannelEntity;
import com.tchalanet.server.catalog.drawchannel.internal.persistence.DrawChannelRepository;
import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.json.utils.JsonUtils;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DrawChannelAdminService {

  private final DrawChannelRepository repository;
  private final DrawChannelMapper mapper;
  private final JsonUtils jsonUtils;

  // helper to ensure flags is never null
  private void ensureFlagsNotNull(DrawChannelEntity e) {
    if (e.getFlags() == null) {
      e.setFlags(jsonUtils.emptyObjectNode());
    }
  }

  @Transactional
  @CacheEvict(
      cacheNames = {
        DrawChannelCacheNames.BY_TENANT,
        DrawChannelCacheNames.BY_ID,
        DrawChannelCacheNames.BY_TENANT_GAME_MAP,
        DrawChannelCacheNames.BY_TENANT_BY_RESULT_SLOT_ID,
        DrawChannelCacheNames.BY_TENANT_BY_RESULT_SLOT_PROVIDER_KEY
      },
      allEntries = true)
  public DrawChannelEntity create(DrawChannelEntity entity) {
    // normalize flags to ensure non-null JsonNode saved to DB and accept textual payload
    var flags = entity.getFlags();
    if (flags == null) {
      flags = jsonUtils.emptyObject();
    } else if (flags.isString()) {
      flags = jsonUtils.parse(flags.asString());
    }
    entity.setFlags(flags);

    return repository.save(entity);
  }

  @Transactional
  @CacheEvict(
      cacheNames = {
        DrawChannelCacheNames.BY_TENANT,
        DrawChannelCacheNames.BY_ID,
        DrawChannelCacheNames.BY_TENANT_GAME_MAP,
        DrawChannelCacheNames.BY_TENANT_BY_RESULT_SLOT_ID,
        DrawChannelCacheNames.BY_TENANT_BY_RESULT_SLOT_PROVIDER_KEY
      },
      allEntries = true)
  public DrawChannelEntity update(UUID id, DrawChannelEntity dto) {
    var existing = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Draw channel not found: " + id));
    if (existing.getDeletedAt() != null) throw new IllegalArgumentException("Draw channel deleted: " + id);
    existing.setName(dto.getName());
    existing.setTimezone(dto.getTimezone());
    existing.setDrawTime(dto.getDrawTime());
    existing.setCutoffSec(dto.getCutoffSec());
    existing.setDaysOfWeek(dto.getDaysOfWeek());
    existing.setActive(dto.isActive());
    existing.setSortOrder(dto.getSortOrder());
    // normalize flags from dto before setting only if dto provides flags; otherwise preserve existing flags
    var dtoFlags = dto.getFlags();
    if (dtoFlags != null) {
      var flags = dtoFlags;
      if (flags.isTextual()) {
        flags = jsonUtils.parse(flags.asText());
      }
      existing.setFlags(flags);
    }
    // ensure flags are not null (important)
    ensureFlagsNotNull(existing);

    existing.setNotes(dto.getNotes());
    existing.setResultSlotId(dto.getResultSlotId());
    existing.setUpdatedAt(Instant.now());
    return repository.save(existing);
  }

  @Transactional
  @CacheEvict(
      cacheNames = {
        DrawChannelCacheNames.BY_TENANT,
        DrawChannelCacheNames.BY_ID,
        DrawChannelCacheNames.BY_TENANT_GAME_MAP,
        DrawChannelCacheNames.BY_TENANT_BY_RESULT_SLOT_ID,
        DrawChannelCacheNames.BY_TENANT_BY_RESULT_SLOT_PROVIDER_KEY
      },
      allEntries = true)
  public void softDelete(com.tchalanet.server.common.types.id.DrawChannelId id) {
    var existing = repository.findById(id.value()).orElseThrow(() -> new IllegalArgumentException("Draw channel not found: " + id));
    var now = Instant.now();
    existing.setDeletedAt(now);
    existing.setUpdatedAt(now);
    // actorId intentionally not stored here; audit handled centrally
    repository.save(existing);
  }

  // mapping helpers
  public DrawChannelView mapToView(DrawChannelEntity e) {
    return mapper.toView(e);
  }

  public DrawChannelEntity mapToEntity(DrawChannelView v) {
    DrawChannelEntity e = new DrawChannelEntity();
    e.setCode(v.code());
    e.setName(v.name());
    e.setTimezone(v.timezone() == null ? null : v.timezone().toString());
    e.setDrawTime(v.drawTime());
    e.setCutoffSec(v.cutoffSec() == null ? 120 : v.cutoffSec());
    // daysOfWeek now List<DayOfWeek> -> format to string
    e.setDaysOfWeek(com.tchalanet.server.common.time.DaysOfWeekFormatter.format(v.daysOfWeek()));
    e.setActive(v.active());
    e.setSortOrder(v.sortOrder());
    e.setFlags(v.flags());
    e.setNotes(v.notes());
    e.setResultSlotId(v.resultSlotId() == null ? null : v.resultSlotId().value());
    return e;
  }

  /**
   * Web -> create: accepte la DTO web, mappe en entity, normalise et persiste, retourne la view
   */
  @Transactional
  public DrawChannelView createFromRequest(
      com.tchalanet.server.catalog.drawchannel.internal.web.model.CreateDrawChannelRequest req) {
    DrawChannelEntity entity = mapper.toEntity(req);
    var created = create(entity);
    return mapToView(created);
  }

  /**
   * Web -> update: accepte la DTO web et l'id typé, mappe en entity et appelle update (merge en service)
   */
  @Transactional
  @CacheEvict(
      cacheNames = {
        DrawChannelCacheNames.BY_TENANT,
        DrawChannelCacheNames.BY_ID,
        DrawChannelCacheNames.BY_TENANT_GAME_MAP,
        DrawChannelCacheNames.BY_TENANT_BY_RESULT_SLOT_ID,
        DrawChannelCacheNames.BY_TENANT_BY_RESULT_SLOT_PROVIDER_KEY
      },
      allEntries = true)
  public DrawChannelView updateFromRequest(
      com.tchalanet.server.common.types.id.DrawChannelId id,
      com.tchalanet.server.catalog.drawchannel.internal.web.model.UpdateDrawChannelRequest req) {
    var existing = repository.findById(id.value()).orElseThrow(() -> new IllegalArgumentException("Draw channel not found: " + id));
    if (existing.getDeletedAt() != null) throw new IllegalArgumentException("Draw channel deleted: " + id);

    // Use mapper to update existing entity in-place (it ignores id/audit/flags)
    mapper.updateEntityFromRequest(req, existing);

    // ensure flags are not null (important)
    ensureFlagsNotNull(existing);

    // handle flags if provided in req: mapper ignores flags so handle explicitly
    // The request type does not have raw JsonNode flags, so if you need flags support in web request, extend the DTO.

    existing.setUpdatedAt(Instant.now());
    var saved = repository.save(existing);
    return mapToView(saved);
  }

  /**
   * Create from view: normalize flags and persist, returning the created View.
   */
  @Transactional
  @CacheEvict(
      cacheNames = {
        DrawChannelCacheNames.BY_TENANT,
        DrawChannelCacheNames.BY_ID,
        DrawChannelCacheNames.BY_TENANT_GAME_MAP,
        DrawChannelCacheNames.BY_TENANT_BY_RESULT_SLOT_ID,
        DrawChannelCacheNames.BY_TENANT_BY_RESULT_SLOT_PROVIDER_KEY
      },
      allEntries = true)
  public DrawChannelView createFromView(
      DrawChannelView view) {
    // normalize flags
    var flags = view.flags();
    if (flags == null) {
      flags = jsonUtils.emptyObjectNode();
    } else if (flags.isTextual()) {
      flags = jsonUtils.parse(flags.asText());
    }

    // map and persist
    var e = mapToEntity(view);
    e.setFlags(flags);
    var created = repository.save(e);
    return mapToView(created);
  }

  @Transactional
  @CacheEvict(
      cacheNames = {
        DrawChannelCacheNames.BY_TENANT,
        DrawChannelCacheNames.BY_ID,
        DrawChannelCacheNames.BY_TENANT_GAME_MAP,
        DrawChannelCacheNames.BY_TENANT_BY_RESULT_SLOT_ID,
        DrawChannelCacheNames.BY_TENANT_BY_RESULT_SLOT_PROVIDER_KEY
      },
      allEntries = true)
  public DrawChannelView updateFromView(
      DrawChannelId id, DrawChannelView view) {
    // normalize flags
    var flags = view.flags();
    if (flags == null) {
      flags = jsonUtils.emptyObjectNode();
    } else if (flags.isTextual()) {
      flags = jsonUtils.parse(flags.asText());
    }

    var dto = mapToEntity(view);
    dto.setFlags(flags);
    var updated = update(id.value(), dto);
    return mapToView(updated);
  }

  /**
   * Patch flags only from web request. Normalizes flags and persists.
   */
  @Transactional
  @CacheEvict(
      cacheNames = {
        DrawChannelCacheNames.BY_TENANT,
        DrawChannelCacheNames.BY_ID,
        DrawChannelCacheNames.BY_TENANT_GAME_MAP,
        DrawChannelCacheNames.BY_TENANT_BY_RESULT_SLOT_ID,
        DrawChannelCacheNames.BY_TENANT_BY_RESULT_SLOT_PROVIDER_KEY
      },
      allEntries = true)
  public DrawChannelView updateFlagsFromRequest(
      com.tchalanet.server.common.types.id.DrawChannelId id,
      com.tchalanet.server.catalog.drawchannel.internal.web.model.UpdateDrawChannelFlagsRequest req) {
    var existing = repository.findById(id.value()).orElseThrow(() -> new IllegalArgumentException("Draw channel not found: " + id));
    if (existing.getDeletedAt() != null) throw new IllegalArgumentException("Draw channel deleted: " + id);

    var flags = req.flags();
    if (flags == null) {
      throw new IllegalArgumentException("flags must be provided and not null");
    } else if (flags.isTextual()) {
      try {
        flags = jsonUtils.parse(flags.asText());
      } catch (Exception ex) {
        throw new IllegalArgumentException("Invalid JSON provided for flags", ex);
      }
    }

    if (flags == null) {
      throw new IllegalArgumentException("flags must be a valid JSON value");
    }

    existing.setFlags(flags);

    existing.setUpdatedAt(Instant.now());
    var saved = repository.save(existing);
    return mapToView(saved);
  }
}
