package com.tchalanet.server.pagemodel.application;

import com.tchalanet.server.pagemodel.domain.model.PageModel;
import com.tchalanet.server.pagemodel.domain.ports.in.GetPageModelUseCase;
import com.tchalanet.server.pagemodel.domain.ports.out.PageModelRepositoryPort;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class GetPageModelService implements GetPageModelUseCase {

  private final PageModelRepositoryPort repositoryPort;

  @Override
  public PageModel getPageModel(UUID tenantId, String code, String lang) {
    String normalizedLang = normalizeLang(lang);
    return repositoryPort
        .findByTenantAndCodeAndLang(tenantId, code, normalizedLang)
        .orElseThrow(
            () ->
                new IllegalStateException(
                    String.format(
                        "No PageModel '%s' for tenant %s and lang=%s",
                        code, tenantId, normalizedLang)));
  }

  private String normalizeLang(String lang) {
    if (lang == null || lang.isBlank()) {
      return "fr";
    }
    return lang.toLowerCase(Locale.ROOT);
  }
}
