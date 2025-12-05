package com.tchalanet.server.features.news.admin;

import com.tchalanet.server.features.news.admin.dto.AdminNewsItemDto;
import com.tchalanet.server.features.news.admin.dto.AdminNewsListResponse;
import com.tchalanet.server.features.news.admin.dto.AdminUpsertNewsRequest;
import com.tchalanet.server.features.news.admin.dto.ChangeStatusRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/news")
@RequiredArgsConstructor
public class AdminNewsController {

    private final AdminNewsService adminNewsService;

    @GetMapping
    public AdminNewsListResponse list() {
        return adminNewsService.list();
    }

    @PostMapping
    public AdminNewsItemDto upsert(@RequestBody AdminUpsertNewsRequest request) {
        return adminNewsService.upsert(request);
    }

    @PostMapping("/{id}/status")
    public AdminNewsItemDto changeStatus(
        @PathVariable UUID id,
        @RequestBody ChangeStatusRequest request
    ) {
        return adminNewsService.changeStatus(id, request.status());
    }

    @PostMapping("/{id}/hide")
    public void hide(@PathVariable String id) {
        adminNewsService.hide(id);
    }

    @PostMapping("/{id}/show")
    public void show(@PathVariable String id) {
        adminNewsService.show(id);
    }

    @PostMapping("/force-refresh")
    public void forceRefresh() {
        adminNewsService.forceRefresh();
    }
}
