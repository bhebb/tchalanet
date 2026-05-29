package com.tchalanet.server.platform.publiccontent.internal.web;

import com.tchalanet.server.platform.audit.api.AuditLog;
import com.tchalanet.server.platform.audit.api.model.AuditAction;
import com.tchalanet.server.platform.audit.api.model.AuditEntityType;
import com.tchalanet.server.platform.publiccontent.api.model.PublicContentAdminItemView;
import com.tchalanet.server.platform.publiccontent.api.model.PublicContentSourceType;
import com.tchalanet.server.platform.publiccontent.internal.news.PublicContentAdminService;
import com.tchalanet.server.platform.publiccontent.internal.news.PublicContentItem;
import com.tchalanet.server.platform.publiccontent.internal.web.model.ChangePublicContentStatusRequest;
import com.tchalanet.server.platform.publiccontent.internal.web.model.UpsertPublicContentRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/platform/public-content/news")
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Platform • Public Content Admin")
public class PlatformPublicContentAdminController {

  private final PublicContentAdminService adminService;

  @Operation(summary = "List all internal public content items")
  @GetMapping
  public List<PublicContentAdminItemView> list() {
    return adminService.listAll().stream().map(this::toAdminView).toList();
  }

  @Operation(summary = "Create or update a public content item")
  @PostMapping
  @AuditLog(entity = AuditEntityType.PUBLIC_CONTENT, action = AuditAction.UPDATE,
      idExpression = "#request.id()", detailsExpression = "#request.title()")
  public PublicContentAdminItemView upsert(@Valid @RequestBody UpsertPublicContentRequest request) {
    var item = adminService.upsert(
        request.id(), request.title(), request.content(), request.contentHtml(),
        request.imageUrl(), request.sourceUrl(), request.status(),
        request.targetSurfaces(), request.publishedAt(), request.expiresAt());
    return toAdminView(item);
  }

  @Operation(summary = "Change status of a public content item")
  @PostMapping("/{id}/status")
  @AuditLog(entity = AuditEntityType.PUBLIC_CONTENT, action = AuditAction.STATE_CHANGE,
      idExpression = "#id", detailsExpression = "#request.status()")
  public PublicContentAdminItemView changeStatus(
      @PathVariable String id,
      @Valid @RequestBody ChangePublicContentStatusRequest request) {
    return toAdminView(adminService.changeStatus(id, request.status()));
  }

  @Operation(summary = "Hide a public content item (admin overlay)")
  @PostMapping("/{id}/hide")
  @AuditLog(entity = AuditEntityType.PUBLIC_CONTENT, action = AuditAction.STATE_CHANGE,
      idExpression = "#id")
  public void hide(@PathVariable String id) {
    adminService.hide(id);
  }

  @Operation(summary = "Show (unhide) a public content item")
  @PostMapping("/{id}/show")
  @AuditLog(entity = AuditEntityType.PUBLIC_CONTENT, action = AuditAction.STATE_CHANGE,
      idExpression = "#id")
  public void show(@PathVariable String id) {
    adminService.show(id);
  }

  @Operation(summary = "Force refresh external RSS feed")
  @PostMapping("/force-refresh")
  public void forceRefresh() {
    adminService.forceRefreshExternal();
  }

  private PublicContentAdminItemView toAdminView(PublicContentItem item) {
    UUID id = toUuid(item.id());
    return new PublicContentAdminItemView(
        id, item.title(), item.content(), item.imageUrl(),
        item.sourceUrl() != null ? item.sourceUrl().toString() : null,
        item.sourceType() != null ? item.sourceType() : PublicContentSourceType.INTERNAL,
        item.status(), item.publishedAt(), item.expiresAt(),
        item.targetSurfaces(),
        null, null, null, null); // audit fields V1: not tracked yet
  }

  private static UUID toUuid(String id) {
    if (id == null) return null;
    try { return UUID.fromString(id); }
    catch (IllegalArgumentException e) { return UUID.nameUUIDFromBytes(id.getBytes()); }
  }
}
