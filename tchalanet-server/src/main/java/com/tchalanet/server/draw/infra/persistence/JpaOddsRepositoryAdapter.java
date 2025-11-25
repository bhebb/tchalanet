package com.tchalanet.server.draw.infra.persistence;

import com.tchalanet.server.draw.domain.model.Odds;
import com.tchalanet.server.draw.domain.ports.OddsRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JpaOddsRepositoryAdapter implements OddsRepository {

  private final OddsJpaRepository jpa;

  @Override
  public Optional<Odds> findById(UUID id) {
    return jpa.findById(id)
        .map(
            e ->
                new Odds(
                    e.getId(),
                    e.getTenantId(),
                    e.getGameCode(),
                    e.getMultiplier(),
                    e.getValidFrom(),
                    e.getValidTo()));
  }

  @Override
  public Odds save(Odds odds) {
    var e = new OddsJpaEntity();
    e.setId(odds.id());
    e.setTenantId(odds.tenantId());
    e.setGameCode(odds.gameCode());
    e.setMultiplier(odds.multiplier());
    e.setValidFrom(odds.validFrom());
    e.setValidTo(odds.validTo());
    var saved = jpa.save(e);
    return new Odds(
        saved.getId(),
        saved.getTenantId(),
        saved.getGameCode(),
        saved.getMultiplier(),
        saved.getValidFrom(),
        saved.getValidTo());
  }
}
