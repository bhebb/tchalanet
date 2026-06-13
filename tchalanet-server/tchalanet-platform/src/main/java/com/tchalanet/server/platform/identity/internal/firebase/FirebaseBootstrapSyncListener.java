package com.tchalanet.server.platform.identity.internal.firebase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnExpression(
    "${tch.identity.firebase.bootstrap.auto-run-on-startup:false} && "
        + "('${tch.identity.provider:firebase}' == 'firebase' || "
        + "'${tch.identity.provider:firebase}' == 'firebase-emulator')")
@RequiredArgsConstructor
@Slf4j
public class FirebaseBootstrapSyncListener {

  private final FirebaseBootstrapSyncService syncService;

  @EventListener(ApplicationReadyEvent.class)
  public void onReady() {
    var result = syncService.syncConfiguredUsers();
    log.info(
        "Firebase bootstrap startup sync finished attempted={} created={} linked={}",
        result.attempted(),
        result.createdInFirebase(),
        result.linked());
  }
}
