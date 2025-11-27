package com.tchalanet.server.pagemodel.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tchalanet.server.pagemodel.domain.ports.in.GetPublicHomePageUseCase;
import com.tchalanet.server.pagemodel.web.dto.PublicHomeResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/home")
@RequiredArgsConstructor
public class PublicPageModelController {

  private final GetPublicHomePageUseCase getPublicHomePageUseCase;
  private final ObjectMapper objectMapper; // To manipulate JSON

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<PublicHomeResponse> getPublicHome(
      @RequestParam(name = "lang", required = false) String langParam,
      @RequestHeader(name = "Accept-Language", required = false) String acceptLang) {
    String lang = resolveLang(langParam, acceptLang);
    GetPublicHomePageUseCase.EnrichedPageModel enrichedPageModel =
        getPublicHomePageUseCase.getPublicHome(lang);

    // Enrich the PageModel JSON with dynamic data
    JsonNode rootNode;
    try {
      rootNode = objectMapper.readTree(enrichedPageModel.pageModel().getJson());
      if (rootNode.isObject()) {
        ObjectNode objNode = (ObjectNode) rootNode;
        objNode.set("dynamicData", objectMapper.valueToTree(enrichedPageModel.dynamicData()));
      }
    } catch (IOException e) {
      throw new IllegalStateException("Failed to parse or enrich PageModel JSON", e);
    }

    PublicHomeResponse response =
        new PublicHomeResponse(
            enrichedPageModel.pageModel().getCode(),
            enrichedPageModel.pageModel().getLang(),
            rootNode // Use the enriched JSON node
            );

    HttpHeaders headers = new HttpHeaders();
    headers.setCacheControl(CacheControl.maxAge(Duration.ofMinutes(10)).cachePublic());
    return ResponseEntity.ok().headers(headers).body(response);
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
}
