package com.tchalanet.server.common.context;

import static com.tchalanet.server.common.constant.ContextKeys.BOOTSTRAPPED_APP_USER_ID;

import com.tchalanet.server.common.security.ApiScope;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ActorContextResolver {

  public TchRequestContext attachBootstrappedAppUserId(
      HttpServletRequest req,
      HttpServletResponse res,
      TchRequestContext ctx)
      throws IOException {

    if (ctx.keycloakUserId() == null || ctx.appUserId() != null) {
      return ctx;
    }

    Object appUserId = req.getAttribute(BOOTSTRAPPED_APP_USER_ID);

    if (appUserId instanceof UUID uuid) {
      return ctx.withAppUserId(uuid);
    }

    if (ctx.apiScope() != ApiScope.PUBLIC) {
      res.sendError(HttpServletResponse.SC_FORBIDDEN, "User bootstrap required");
      return null;
    }

    return ctx;
  }
}
