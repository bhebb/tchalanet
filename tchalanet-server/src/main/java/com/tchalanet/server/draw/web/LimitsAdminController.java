package com.tchalanet.server.draw.web;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST API d'administration pour la gestion des limites de tickets. TODO: Migration hexagonale
 * complète - implémentation temporaire.
 */
@RestController
@RequestMapping("/api/v1/admin/limits")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
public class LimitsAdminController {

  @GetMapping
  public ResponseEntity<List<Map<String, Object>>> listLimits() {
    // TODO: Implémenter avec le use case
    return ResponseEntity.ok(
        List.of(Map.of("scope", "NUMBER", "pattern", "*11*", "daily_cap", 5000)));
  }

  @PutMapping("/{id}")
  public ResponseEntity<Map<String, Object>> updateLimit(
      @PathVariable String id, @RequestBody Map<String, Object> body) {
    // TODO: Implémenter avec le use case
    return ResponseEntity.ok(Map.of("status", "updated", "id", id));
  }
}
