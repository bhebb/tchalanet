package com.tchalanet.server.core.pagemodel.internal.domain.policy;

import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.pagemodel.domain.model.PageModelInstance;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class PublishPolicy {

  /**
   * Enforce: only one PUBLISHED per logicalId (within tenant context).
   * Strategy: archive other published, publish limitScopeRef.
   */
  public List<PageModelInstance> apply(
      PageModelInstance toPublish,
      List<PageModelInstance> currentlyPublished,
      Instant now,
      UUID actorId) {

    List<PageModelInstance> changed = new ArrayList<>();

    for (var other : currentlyPublished) {
      if (!other.id().equals(toPublish.id())) {
        other.markArchived(now, UserId.of(actorId));
        changed.add(other);
      }
    }

    toPublish.markPublished(now, UserId.of(actorId));
    changed.add(toPublish);

    return changed;
  }
}
