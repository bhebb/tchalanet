package com.tchalanet.server.catalog.pagemodeltemplate.internal.init;

import com.tchalanet.server.catalog.pagemodeltemplate.internal.write.PageModelTemplateAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads static PageModelTemplate JSON files and upserts them into the template catalog.
 *
 * The write port is intentionally small. Implement it in catalog.pagemodeltemplate internal
 * infrastructure/write layer, not in features.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PageModelTemplateSeedService {

  private final StaticPageModelTemplateLoader loader;
  private final PageModelTemplateAdminService writer;

  @Transactional
  public void seedSystemTemplates() {
    var templates = loader.loadTemplates();

    if (templates.isEmpty()) {
      log.warn("No static PageModelTemplate files found. Check classpath:/pagemodel/templates/");
      return;
    }

    for (var tpl : templates) {
      writer.upsertGlobalFromSeed(tpl);
      log.info("Seeded PageModelTemplate logicalId={} code={}", tpl.logicalId(), tpl.code());
    }
  }
}
