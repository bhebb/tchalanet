package com.tchalanet.server.services.subscription;

import com.tchalanet.server.dto.ChangePlanRequest;
import com.tchalanet.server.dto.PlanDTO;
import com.tchalanet.server.dto.SubscriptionDTO;
import com.tchalanet.server.error.ProblemRest;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile({"local", "test"})
public class MockSubscriptionService implements ISubscription {

  private record InMemorySub(
      UUID id,
      String tenantId,
      PlanDTO plan,
      String status,
      Instant start,
      Instant end,
      boolean cancelAtPeriodEnd,
      String provider,
      String providerSubId) {}

  // “Catalogue” simulé (id → PlanDTO)
  private final Map<UUID, PlanDTO> plans = new LinkedHashMap<>();

  // Abonnements par tenant
  private final Map<String, InMemorySub> subs = new HashMap<>();

  public MockSubscriptionService() {
    // Seed plans
    PlanDTO basic =
        new PlanDTO(
            UUID.randomUUID(),
            "BASIC",
            BigDecimal.ZERO,
            "EUR",
            "MONTH",
            true,
            List.of("plans.feat.public_pages", "plans.feat.theming"));
    PlanDTO pro =
        new PlanDTO(
            UUID.randomUUID(),
            "PRO",
            new BigDecimal("19.00"),
            "EUR",
            "MONTH",
            true,
            List.of("plans.feat.public_pages", "plans.feat.theming", "plans.feat.analytics"));
    PlanDTO ent =
        new PlanDTO(
            UUID.randomUUID(),
            "ENTERPRISE",
            new BigDecimal("49.00"),
            "EUR",
            "MONTH",
            true,
            List.of(
                "plans.feat.public_pages",
                "plans.feat.theming",
                "plans.feat.analytics",
                "plans.feat.sso"));
    plans.put(basic.id(), basic);
    plans.put(pro.id(), pro);
    plans.put(ent.id(), ent);

    // Seed un tenant
    String tenantSeed = "tenant-dev-1";
    subs.put(
        tenantSeed,
        new InMemorySub(
            UUID.randomUUID(),
            tenantSeed,
            basic,
            "ACTIVE",
            Instant.now().truncatedTo(ChronoUnit.SECONDS),
            Instant.now().plus(30, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS),
            false,
            "NONE",
            "noop-" + tenantSeed));
  }

  @Override
  public SubscriptionDTO currentForTenant(String tenantId) {
    InMemorySub s = subs.get(tenantId);
    if (s == null) throw ProblemRest.notFound("No active subscription for tenant=" + tenantId);
    return toDto(s);
  }

  @Override
  public SubscriptionDTO changePlan(String tenantId, ChangePlanRequest req) {
    InMemorySub cur = subs.get(tenantId);
    PlanDTO target = plans.get(req.planId());
    if (target == null) throw ProblemRest.notFound("Plan not found: " + req.planId());

    // Idempotence simple (ex: on pourrait garder les dernières keys par tenant)
    // Ici on fait simple: pas de stockage, mais tu peux étendre.

    // Upgrade/downgrade “immédiat” en mock
    InMemorySub next =
        new InMemorySub(
            (cur != null ? cur.id() : UUID.randomUUID()),
            tenantId,
            target,
            "ACTIVE",
            Instant.now().truncatedTo(ChronoUnit.SECONDS),
            Instant.now().plus(30, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS),
            false,
            "NONE",
            "noop-" + tenantId);

    subs.put(tenantId, next);
    return toDto(next);
  }

  @Override
  public SubscriptionDTO cancel(String tenantId, boolean atPeriodEnd) {
    InMemorySub cur = subs.get(tenantId);
    if (cur == null) throw ProblemRest.notFound("No active subscription for tenant=" + tenantId);

    InMemorySub next =
        atPeriodEnd
            ? new InMemorySub(
                cur.id(),
                tenantId,
                cur.plan(),
                cur.status(),
                cur.start(),
                cur.end(),
                true,
                cur.provider(),
                cur.providerSubId())
            : new InMemorySub(
                cur.id(),
                tenantId,
                cur.plan(),
                "CANCELED",
                cur.start(),
                Instant.now().truncatedTo(ChronoUnit.SECONDS),
                false,
                cur.provider(),
                cur.providerSubId());

    subs.put(tenantId, next);
    return toDto(next);
  }

  @Override
  public SubscriptionDTO resume(String tenantId) {
    InMemorySub cur = subs.get(tenantId);
    if (cur == null) throw ProblemRest.notFound("No subscription for tenant=" + tenantId);

    // Si cancelAtPeriodEnd → annule le flag et repart sur une nouvelle période
    InMemorySub next =
        new InMemorySub(
            cur.id(),
            tenantId,
            cur.plan(),
            "ACTIVE",
            Instant.now().truncatedTo(ChronoUnit.SECONDS),
            Instant.now().plus(30, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS),
            false,
            cur.provider(),
            cur.providerSubId());

    subs.put(tenantId, next);
    return toDto(next);
  }

  private static SubscriptionDTO toDto(InMemorySub s) {
    return new SubscriptionDTO(
        s.id(),
        s.tenantId(),
        s.plan(),
        s.status(),
        s.start(),
        s.end(),
        s.cancelAtPeriodEnd(),
        s.provider(),
        s.providerSubId());
  }
}
