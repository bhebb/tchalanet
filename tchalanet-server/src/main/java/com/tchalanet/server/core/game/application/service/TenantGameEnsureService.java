package com.tchalanet.server.core.game.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tchalanet.server.core.game.infra.persistence.GameJpaRepository;
import com.tchalanet.server.core.game.infra.persistence.TenantGameJpaEntity;
import com.tchalanet.server.core.game.infra.persistence.TenantGameJpaRepository;
import jakarta.transaction.Transactional;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantGameEnsureService {

  private final GameJpaRepository gameRepo;
  private final TenantGameJpaRepository tenantGameRepo;
  private final ObjectMapper objectMapper;

  public record EnsureResult(
      List<String> requestedCodes, List<String> createdCodes, List<String> alreadyAssignedCodes) {}

  @Transactional
  public EnsureResult ensureByGameCodes(List<String> gameCodes) {
    var requested = normalizeCodes(gameCodes);
    if (requested.isEmpty()) {
      return new EnsureResult(List.of(), List.of(), List.of());
    }

    // 1) Load all games by code (strict validation)
    var gamesByCode = new HashMap<String, UUID>(requested.size());
    var unknown = new ArrayList<String>();

    for (String code : requested) {
      var gOpt = gameRepo.findByCode(code);
      if (gOpt.isEmpty()) {
        unknown.add(code);
      } else {
        gamesByCode.put(code, gOpt.get().getId());
      }
    }

    if (!unknown.isEmpty()) {
      // strict mode: don't create partial assignments
      throw new IllegalArgumentException("Unknown game codes: " + unknown);
    }

    // 2) Ensure rows exist for those codes only
    var created = new ArrayList<String>();
    var already = new ArrayList<String>();

    for (String code : requested) {
      var exists = tenantGameRepo.findByGame_CodeAndDeletedAtIsNull(code).isPresent();
      if (exists) {
        already.add(code);
        continue;
      }

      var g = gameRepo.findByCode(code).orElseThrow(); // safe after strict check

      TenantGameJpaEntity tg = new TenantGameJpaEntity();
      tg.setGame(g);
      tg.setEnabled(true);
      tg.setDisplayName(g.getName());
      tg.setFlags(Collections.emptyMap());

      try {
        tenantGameRepo.save(tg);
        created.add(code);
      } catch (DataIntegrityViolationException dive) {
        // race/duplicate => treat as already assigned
        already.add(code);
        log.debug("tenant_game already exists for code={}: {}", code, dive.getMessage());
      }
    }

    log.info(
        "TenantGameEnsureService: ensureByGameCodes requested={}, created={}, already={}",
        requested.size(),
        created.size(),
        already.size());

    return new EnsureResult(requested, created, already);
  }

  private List<String> normalizeCodes(List<String> codes) {
    if (codes == null) return List.of();
    return codes.stream()
        .filter(Objects::nonNull)
        .map(String::trim)
        .filter(s -> !s.isBlank())
        .map(String::toUpperCase)
        .distinct()
        .toList();
  }
}
