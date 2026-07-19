package com.tchalanet.server.catalog.game.api.error;

import com.tchalanet.server.common.web.error.ErrorDescriptor;
import com.tchalanet.server.common.web.error.ErrorDescriptorContributor;
import java.util.Set;
import org.springframework.stereotype.Component;

/** Registers game catalog errors with the application-wide descriptor registry. */
@Component
class GameCatalogErrorDescriptorContributor implements ErrorDescriptorContributor {

  @Override
  public String owner() {
    return "catalog.game";
  }

  @Override
  public Set<ErrorDescriptor> descriptors() {
    return GameCatalogErrorCodes.all();
  }
}
