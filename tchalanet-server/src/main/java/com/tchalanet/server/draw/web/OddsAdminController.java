package com.tchalanet.server.draw.web;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST API d'administration pour la gestion des cotes (odds). TODO: Migration hexagonale complète -
 * implémentation temporaire.
 */
@RestController
@RequestMapping("/api/v1/admin/odds")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
public class OddsAdminController {

  @GetMapping
  public ResponseEntity<List<Map<String, Object>>> listOdds(
      @RequestParam(required = false) String game) {
    // TODO: Implémenter avec le use case
    return ResponseEntity.ok(
        List.of(Map.of("game", "4C", "multiplier", 25.0, "valid_from", "2025-01-01")));
  }

  @PutMapping("/{game}")
  public ResponseEntity<Map<String, Object>> upsertOdds(
      @PathVariable String game, @RequestBody Map<String, Object> body) {
    // TODO: Implémenter avec le use case - valider & sauvegarder + Envers + audit_event
    return ResponseEntity.ok(Map.of("status", "updated", "game", game));
  }
}
