package com.tchalanet.server.draw.web;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST API d'administration pour la gestion des tirages. TODO: Migration hexagonale complète -
 * implémentation temporaire.
 */
@RestController
@RequestMapping("/api/v1/admin/draws")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
public class DrawAdminController {

  @GetMapping
  public ResponseEntity<List<Map<String, Object>>> listDraws() {
    // TODO: Implémenter avec le use case
    return ResponseEntity.ok(
        List.of(
            Map.of(
                "draw_id",
                "NY-2025-08-31-12",
                "scheduled_at",
                "2025-08-31T12:00:00Z",
                "cutoff_sec",
                120)));
  }

  @PostMapping
  public ResponseEntity<Map<String, Object>> createDraw(@RequestBody Map<String, Object> body) {
    // TODO: Implémenter avec le use case
    return ResponseEntity.ok(Map.of("status", "created", "id", "DRAW-123"));
  }

  @PutMapping("/{id}")
  public ResponseEntity<Map<String, Object>> updateDraw(
      @PathVariable String id, @RequestBody Map<String, Object> body) {
    // TODO: Implémenter avec le use case
    return ResponseEntity.ok(Map.of("status", "updated", "id", id));
  }
}
