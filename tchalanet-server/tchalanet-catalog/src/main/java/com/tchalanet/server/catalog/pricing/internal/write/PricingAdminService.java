package com.tchalanet.server.catalog.pricing.internal.write;

import com.tchalanet.server.catalog.pricing.internal.cache.PricingCacheNames;
import com.tchalanet.server.catalog.pricing.internal.mapper.PricingEntityMapper;
import com.tchalanet.server.catalog.pricing.internal.persistence.PricingOddsEntity;
import com.tchalanet.server.catalog.pricing.internal.persistence.PricingOddsJpaRepository;
import com.tchalanet.server.catalog.pricing.internal.web.model.CreatePricingOddsRequest;
import com.tchalanet.server.catalog.pricing.internal.web.model.UpdatePricingOddsRequest;
import com.tchalanet.server.catalog.pricing.internal.web.model.PricingOddsView;
import com.tchalanet.server.common.types.id.PricingOddsId;
import com.tchalanet.server.common.types.id.TenantId;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PricingAdminService {

  private final PricingOddsJpaRepository repo;
  private final PricingEntityMapper mapper;

  @Transactional
  @CacheEvict(cacheNames = {PricingCacheNames.ODDS}, allEntries = true)
  public PricingOddsView create(CreatePricingOddsRequest req) {
    var e = new PricingOddsEntity();
    apply(req, e);
    e.setActive(req.active() == null || req.active());
    if (req.tenantId() != null) e.setTenantId(req.tenantId().value());
    var saved = repo.save(e);
    return mapper.toView(saved);
  }

  @Transactional
  @CacheEvict(cacheNames = {PricingCacheNames.ODDS}, allEntries = true)
  public PricingOddsView update(PricingOddsId id, UpdatePricingOddsRequest req) {
    UUID uuid = id == null ? null : id.value();
    var e = repo.findById(uuid).filter(x -> x.getDeletedAt() == null).orElseThrow(() -> new RuntimeException("pricing_odds_not_found id=" + uuid));
    apply(req, e);
    if (req.active() != null) e.setActive(req.active());
    var saved = repo.save(e);
    return mapper.toView(saved);
  }

  @Transactional
  @CacheEvict(cacheNames = {PricingCacheNames.ODDS}, allEntries = true)
  public void softDelete(PricingOddsId id) {
    UUID uuid = id == null ? null : id.value();
    var e = repo.findById(uuid).filter(x -> x.getDeletedAt() == null).orElseThrow(() -> new RuntimeException("pricing_odds_not_found id=" + uuid));
    e.setDeletedAt(Instant.now());
    repo.save(e);
  }

  @Transactional(readOnly = true)
  public List<PricingOddsView> listActive() {
    return repo.findAll().stream().filter(e -> e.isActive() && e.getDeletedAt() == null).map(mapper::toView).toList();
  }

  @Transactional(readOnly = true)
  public Optional<PricingOddsView> findById(PricingOddsId id) {
    UUID uuid = id == null ? null : id.value();
    return repo.findById(uuid).filter(x -> x.getDeletedAt() == null).map(mapper::toView);
  }

  private static void apply(com.tchalanet.server.catalog.pricing.internal.web.model.BasePricingOddsRequest req, PricingOddsEntity e) {
    if (req.gameCode() != null) e.setGameCode(req.gameCode().trim().toUpperCase());
    if (req.betType() != null) e.setBetType(req.betType());
    if (req.betOption() != null) e.setBetOption(req.betOption());
    if (req.odds() != null) e.setOdds(req.odds());
  }
}
