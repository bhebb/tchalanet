package com.tchalanet.server.common.web;

import static java.util.stream.Collectors.*;

import com.tchalanet.server.audit.domain.model.AuditAction;
import com.tchalanet.server.audit.domain.model.AuditActorType;
import com.tchalanet.server.audit.domain.model.AuditEntityType;
import com.tchalanet.server.audit.domain.model.AuditEvent;
import com.tchalanet.server.audit.domain.usecase.LogAuditEventUseCase;
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
@RequestMapping("/api/platform/cache")
public class CacheAdminController {

  private final CacheManager cacheManager;
  private final LogAuditEventUseCase audit;

  public CacheAdminController(CacheManager cacheManager, LogAuditEventUseCase audit) {
    this.cacheManager = cacheManager;
    this.audit = audit;
  }

  @PreAuthorize("hasRole('SUPER_ADMIN')")
  @GetMapping("/list")
  public ResponseEntity<List<String>> listCaches() {
    var names = cacheManager.getCacheNames().stream().sorted().collect(toList());
    return ResponseEntity.ok(names);
  }

  @PreAuthorize("hasRole('SUPER_ADMIN')")
  @DeleteMapping("/clear/{cacheName}")
  public ResponseEntity<Void> clearCache(@PathVariable("cacheName") String cacheName) {
    var cache = cacheManager.getCache(cacheName);
    if (cache != null) {
      cache.clear();
      // audit
      var ev =
          AuditEvent.of(
              null,
              AuditActorType.USER,
              "admin",
              AuditEntityType.CACHE,
              cacheName,
              AuditAction.CACHE_CLEAR,
              Map.of("by", "admin").toString(),
              null,
              null);
      audit.log(ev);
    }
    return ResponseEntity.noContent().build();
  }

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
    var evAll =
        AuditEvent.of(
            null,
            AuditActorType.USER,
            "admin",
            AuditEntityType.CACHE,
            "*",
            AuditAction.CACHE_CLEAR,
            Map.of("action", "clear-all").toString(),
            null,
            null);
    audit.log(evAll);
    return ResponseEntity.noContent().build();
  }
}
