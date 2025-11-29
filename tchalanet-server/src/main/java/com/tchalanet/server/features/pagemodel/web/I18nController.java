package com.tchalanet.server.features.pagemodel.web;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.tchalanet.server.common.context.CurrentContext;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.features.pagemodel.application.port.in.GetI18nConfigUseCase;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** REST API pour la récupération des configurations i18n. */
@RestController
@RequestMapping(value = "/configs/i18n", produces = APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class I18nController {

  private final GetI18nConfigUseCase getI18nConfigUseCase;

  @GetMapping
  public ResponseEntity<Map<String, Object>> getI18nConfig(
      @CurrentContext TchRequestContext context, @RequestParam(required = false) String lang) {

    Map<String, Object> i18nConfig = getI18nConfigUseCase.execute(context, lang);
    return ResponseEntity.ok(i18nConfig);
  }
}
