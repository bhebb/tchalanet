package com.tchalanet.server.api.admin;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/admin/odds")
public class OddsAdminController {

    // GET /api/admin/odds?game=xyz
    @GetMapping
    public List<Map<String,Object>> list(@RequestParam(required=false) String game) {
        // TODO: service JPA
        return List.of(Map.of("game","4C","multiplier",25.0,"valid_from","2025-01-01"));
    }

    // PUT /api/admin/odds/{game}
    @PutMapping("/{game}")
    public ResponseEntity<?> upsert(@PathVariable String game,
                                    @RequestBody Map<String,Object> body) {
        // TODO: validate & save + Envers + audit_event
        return ResponseEntity.ok(Map.of("status","updated","game",game));
    }
}

@RestController
@RequestMapping("/api/admin/limits")
class LimitsAdminController {

    @GetMapping
    public List<Map<String,Object>> list() {
        return List.of(Map.of("scope","NUMBER","pattern","*11*","daily_cap", 5000));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody Map<String,Object> body) {
        return ResponseEntity.ok(Map.of("status","updated","id",id));
    }
}

@RestController
@RequestMapping("/api/admin/draws")
class DrawsAdminController {

    @GetMapping
    public List<Map<String,Object>> list() {
        return List.of(Map.of("draw_id","NY-2025-08-31-12","scheduled_at","2025-08-31T12:00:00Z","cutoff_sec",120));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String,Object> body) {
        return ResponseEntity.ok(Map.of("status","created","id","DRAW-123"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody Map<String,Object> body) {
        return ResponseEntity.ok(Map.of("status","updated","id",id));
    }
}
