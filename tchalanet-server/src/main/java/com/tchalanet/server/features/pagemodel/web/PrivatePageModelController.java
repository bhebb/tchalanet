package com.tchalanet.server.features.pagemodel.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tchalanet.server.core.accesscontrol.application.port.in.CheckUserPermissionsUseCase;
import com.tchalanet.server.features.pagemodel.domain.model.PageModel;
import com.tchalanet.server.features.pagemodel.domain.ports.in.GetPageModelUseCase;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Note: `GetPageModelUseCase` is the application use-case used to retrieve private page models.
@RestController
@RequestMapping("/api/v1/pages")
@RequiredArgsConstructor
public class PrivatePageModelController {

  private final GetPageModelUseCase getPageModelUseCase;
  private final CheckUserPermissionsUseCase accessChecker;
  private final ObjectMapper objectMapper;

  // --- Private Pages / Dashboards ---
  @GetMapping(value = "/{pageCode}", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<JsonNode> getPrivatePage(
      @PathVariable String pageCode,
      @RequestHeader("X-Tenant-Id") UUID tenantId,
      @RequestHeader("X-User-Id") UUID userId,
      @RequestParam(name = "lang", required = false) String langParam,
      @RequestHeader(name = "Accept-Language", required = false) String acceptLang) {
    // Access Control: Check if user has permission to view private pages/dashboards
    accessChecker.check(tenantId, userId, List.of("page.view_private"));

    String lang = resolveLang(langParam, acceptLang);
    PageModel pageModel = getPageModelUseCase.getPageModel(tenantId, pageCode, lang);

    // Further enrichment of JSON can happen here if needed, similar to old PageController
    JsonNode enrichedJson =
        objectMapper.valueToTree(
            pageModel.getJson()); // Assuming getJson() returns a valid JSON string

    HttpHeaders headers = new HttpHeaders();
    headers.setCacheControl(CacheControl.maxAge(Duration.ofMinutes(10)).cachePrivate());
    return ResponseEntity.ok().headers(headers).body(enrichedJson);
  }

  private String resolveLang(String langParam, String acceptLang) {
    if (langParam != null && !langParam.isBlank()) {
      return langParam;
    }
    if (acceptLang != null && !acceptLang.isBlank()) {
      return acceptLang.substring(0, 2).toLowerCase(Locale.ROOT);
    }
    return "fr";
  }

  @ResponseStatus(HttpStatus.NOT_FOUND)
  public static class PageModelNotFoundException extends RuntimeException {
    public PageModelNotFoundException(String message) {
      super(message);
    }
  }
}
