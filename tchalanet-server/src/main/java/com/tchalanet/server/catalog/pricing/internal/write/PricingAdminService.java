package com.tchalanet.server.catalog.pricing.internal.write;

import com.tchalanet.server.catalog.pricing.internal.cache.PricingCacheNames;
import com.tchalanet.server.catalog.pricing.internal.persistence.PricingOddsEntity;
import com.tchalanet.server.catalog.pricing.internal.persistence.PricingOddsJpaRepository;
import com.tchalanet.server.catalog.pricing.internal.web.model.CreatePricingOddsRequest;
import com.tchalanet.server.catalog.pricing.internal.web.model.UpdatePricingOddsRequest;
import com.tchalanet.server.common.types.enums.BetType;
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

  @Transactional
  @CacheEvict(cacheNames = {PricingCacheNames.ODDS}, allEntries = true)
  public PricingOddsEntity create(CreatePricingOddsRequest req) {
    var e = new PricingOddsEntity();
    apply(req, e);
    e.setActive(req.active() == null || req.active());
    if (req.tenantId() != null) e.setTenantId(req.tenantId().value());
    return repo.save(e);
  }

  @Transactional
  @CacheEvict(cacheNames = {PricingCacheNames.ODDS}, allEntries = true)
  public PricingOddsEntity update(UUID id, UpdatePricingOddsRequest req) {
    var e = repo.findById(id).filter(x -> x.getDeletedAt() == null).orElseThrow(() -> new RuntimeException("pricing_odds_not_found id=" + id));
    apply(req, e);
    if (req.active() != null) e.setActive(req.active().booleanValue());
    return repo.save(e);
  }

  @Transactional
  @CacheEvict(cacheNames = {PricingCacheNames.ODDS}, allEntries = true)
  public void softDelete(UUID id) {
    var e = repo.findById(id).filter(x -> x.getDeletedAt() == null).orElseThrow(() -> new RuntimeException("pricing_odds_not_found id=" + id));
    e.setDeletedAt(Instant.now());
    repo.save(e);
  }

  @Transactional(readOnly = true)
  public List<PricingOddsEntity> listActive() {
    return repo.findAll().stream().filter(e -> e.isActive() && e.getDeletedAt() == null).toList();
  }

  @Transactional(readOnly = true)
  public Optional<PricingOddsEntity> findById(UUID id) {
    return repo.findById(id).filter(x -> x.getDeletedAt() == null);
  }

  private static void apply(com.tchalanet.server.catalog.pricing.internal.web.model.BasePricingOddsRequest req, PricingOddsEntity e) {
    if (req.gameCode() != null) e.setGameCode(req.gameCode().trim().toUpperCase());
    if (req.betType() != null) e.setBetType(req.betType());
    if (req.betOption() != null) e.setBetOption(req.betOption());
    if (req.odds() != null) e.setOdds(req.odds());
  }
}
