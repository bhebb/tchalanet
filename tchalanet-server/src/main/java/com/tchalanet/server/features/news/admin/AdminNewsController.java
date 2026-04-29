package com.tchalanet.server.features.news.admin;

import com.tchalanet.server.features.news.admin.model.AdminNewsItem;
import com.tchalanet.server.features.news.admin.model.AdminNewsListResponse;
import com.tchalanet.server.features.news.admin.model.AdminUpsertNewsRequest;
import com.tchalanet.server.features.news.admin.model.ChangeStatusRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
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
@RequestMapping("/admin/news")
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
@RequiredArgsConstructor
@Tags({@Tag(name = "Admin • News")})
public class AdminNewsController {

  private final AdminNewsService adminNewsService;

  @Operation(summary = "List admin news items")
  @GetMapping
  public AdminNewsListResponse list() {
    return adminNewsService.list();
  }

  @Operation(summary = "Create or update an admin news item")
  @PostMapping
  public AdminNewsItem upsert(@RequestBody AdminUpsertNewsRequest request) {
    return adminNewsService.upsert(request);
  }

  @Operation(summary = "Change status of a news item")
  @PostMapping("/{id}/status")
  public AdminNewsItem changeStatus(
      @PathVariable UUID id, @RequestBody ChangeStatusRequest request) {
    return adminNewsService.changeStatus(id, request.status());
  }

  @Operation(summary = "Hide a news item")
  @PostMapping("/{id}/hide")
  public void hide(@PathVariable String id) {
    adminNewsService.hide(id);
  }

  @Operation(summary = "Show a news item")
  @PostMapping("/{id}/show")
  public void show(@PathVariable String id) {
    adminNewsService.show(id);
  }

  @Operation(summary = "Force refresh of news cache")
  @PostMapping("/force-refresh")
  public void forceRefresh() {
    adminNewsService.forceRefresh();
  }
}
