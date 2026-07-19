package com.tchalanet.server.features.pos.home.app;

import com.tchalanet.server.common.context.TchActorType;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.features.pos.error.PosErrorCodes;
import com.tchalanet.server.platform.identity.api.model.surface.ClientSurface;
import com.tchalanet.server.platform.identity.api.model.surface.ClientSurfacePolicy;
import org.springframework.stereotype.Component;

@Component
public class ClientSurfaceResolver {

  public static final String HEADER_NAME = "X-Tch-Surface";

  public ClientSurface resolve(TchRequestContext ctx, String requestedSurface) {
    if (ctx != null && ctx.actorType() == TchActorType.SELLER_TERMINAL) {
      var surface =
          requestedSurface == null || requestedSurface.isBlank()
              ? ClientSurface.MOBILE_POS
              : parse(requestedSurface);
      if (surface != ClientSurface.MOBILE_POS) {
        throw ProblemRest.of(PosErrorCodes.SURFACE_NOT_ALLOWED);
      }
      return surface;
    }

    var available = ClientSurfacePolicy.availableSurfaces(ctx.systemRoles());
    var surface =
        requestedSurface == null || requestedSurface.isBlank()
            ? ClientSurfacePolicy.preferredSurface(ctx.systemRoles())
            : parse(requestedSurface);
    if (!available.contains(surface)) {
      throw ProblemRest.of(PosErrorCodes.SURFACE_NOT_ALLOWED);
    }
    return surface;
  }

  private ClientSurface parse(String requestedSurface) {
    try {
      return ClientSurface.valueOf(requestedSurface.trim().toUpperCase(java.util.Locale.ROOT));
    } catch (IllegalArgumentException ex) {
      throw ProblemRest.of(PosErrorCodes.SURFACE_NOT_ALLOWED, ex);
    }
  }
}
