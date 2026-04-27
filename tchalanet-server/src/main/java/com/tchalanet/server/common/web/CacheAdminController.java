package com.tchalanet.server.common.web;

import com.tchalanet.server.common.types.enums.AuditAction;
import com.tchalanet.server.common.types.enums.AuditEntityType;
import com.tchalanet.server.core.audit.application.command.handler.AuditLoggingCommandHandler;
import com.tchalanet.server.core.audit.application.command.model.LogAuditEventCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/platform/cache")
@Tag(name = "Ops • Cache")
public class CacheAdminController {

  private final CacheManager cacheManager;
  private final AuditLoggingCommandHandler audit;

  public CacheAdminController(CacheManager cacheManager, AuditLoggingCommandHandler audit) {
    this.cacheManager = cacheManager;
    this.audit = audit;
  }

  @Operation(summary = "List cache names (ops)")
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  @GetMapping("/list")
  public ResponseEntity<List<String>> listCaches() {
    var names = cacheManager.getCacheNames().stream().sorted().toList();
    return ResponseEntity.ok(names);
  }

  @Operation(summary = "Clear a named cache (ops)")
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  @DeleteMapping("/clear/{cacheName}")
  public ResponseEntity<Void> clearCache(@PathVariable("cacheName") String cacheName) {
    var cache = cacheManager.getCache(cacheName);
    if (cache != null) {
      cache.clear();
      // audit
      var details = Map.<String, Object>of("by", "admin");
      audit.handle(
          new LogAuditEventCommand(
              AuditEntityType.CACHE, cacheName, AuditAction.CACHE_CLEAR, details));
    }
    return ResponseEntity.noContent().build();
  }

  @Operation(summary = "Clear all caches (ops)")
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  @DeleteMapping("/clear-all")
  public ResponseEntity<Void> clearAll() {
    cacheManager
        .getCacheNames()
        .forEach(
            name -> {
              var c = cacheManager.getCache(name);
              if (c != null) {
                c.clear();
              }
            });
    // audit
    var detailsAll = Map.<String, Object>of("action", "clear-all");
    audit.handle(
        new LogAuditEventCommand(AuditEntityType.CACHE, "*", AuditAction.CACHE_CLEAR, detailsAll));
    return ResponseEntity.noContent().build();
  }
}
