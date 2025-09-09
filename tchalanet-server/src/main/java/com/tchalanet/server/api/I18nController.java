package com.tchalanet.server.api;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.tchalanet.server.config.context.CurrentContext;
import com.tchalanet.server.config.context.RequestContext;
import com.tchalanet.server.services.I18nConfigurationService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/configs/i18n", produces = APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class I18nController {

  private final I18nConfigurationService i18nConfigurationService;

  @GetMapping()
  public Map<String, Object> get(
      @CurrentContext RequestContext context, @RequestParam(required = false) String lang) {
    return i18nConfigurationService.getMergedI18n(context, lang);
  }
}
