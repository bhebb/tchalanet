package com.tchalanet.server.platform.identity.internal.firebase;

import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.platform.identity.api.IdentityProviderType;
import com.tchalanet.server.platform.identity.internal.service.ExternalIdentityLinkService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnExpression(
    "'${tch.identity.provider:firebase}' == 'firebase' || "
        + "'${tch.identity.provider:firebase}' == 'firebase-emulator'")
@RequiredArgsConstructor
@Slf4j
public class FirebaseBootstrapSyncService {

  private final FirebaseBootstrapProperties bootstrapProperties;
  private final FirebaseIdentityProperties identityProperties;
  private final FirebaseUserProvisionService firebaseUsers;
  private final ExternalIdentityLinkService externalIdentityLinks;
  private final JdbcTemplate jdbc;

  @Transactional
  public FirebaseBootstrapSyncResult syncConfiguredUsers() {
    if (!bootstrapProperties.enabled()) {
      throw new IllegalStateException("Firebase bootstrap is disabled");
    }

    int attempted = 0;
    int createdInFirebase = 0;
    int linked = 0;

    for (var username : bootstrapProperties.users()) {
      attempted++;
      var localUsers = findLocalUsers(username);
      if (localUsers.isEmpty()) {
        log.warn("Firebase bootstrap skipped missing Tchalanet user username={}", username);
        continue;
      }

      var localUser = localUsers.getFirst();
      var provisioned =
          firebaseUsers.provisionUser(
              localUser.id().toString(),
              localUser.email(),
              localUser.phone(),
              localUser.displayName(),
              bootstrapProperties.defaultUserPassword());
      if (provisioned.created()) {
        createdInFirebase++;
      }
      externalIdentityLinks.link(
          UserId.of(localUser.id()),
          IdentityProviderType.FIREBASE,
          identityProperties.issuer(),
          provisioned.uid(),
          localUser.email());
      linked++;
    }

    log.info(
        "Firebase bootstrap sync done attempted={} createdInFirebase={} linked={}",
        attempted,
        createdInFirebase,
        linked);
    return new FirebaseBootstrapSyncResult(attempted, createdInFirebase, linked);
  }

  private List<BootstrapUser> findLocalUsers(String username) {
    return jdbc.query(
        """
            select id, email, phone, display_name
              from app_user
             where username = ?
               and deleted_at is null
            """,
        (rs, rowNum) ->
            new BootstrapUser(
                rs.getObject("id", UUID.class),
                rs.getString("email"),
                rs.getString("phone"),
                rs.getString("display_name")),
        username);
  }

  private record BootstrapUser(UUID id, String email, String phone, String displayName) {}

  public record FirebaseBootstrapSyncResult(int attempted, int createdInFirebase, int linked) {}
}
