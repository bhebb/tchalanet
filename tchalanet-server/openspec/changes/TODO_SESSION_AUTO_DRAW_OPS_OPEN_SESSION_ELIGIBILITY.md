# TODO — Fix Session Auto, Draw Ops, and OpenSalesSession Eligibility

## Objectif

Nettoyer les flows scheduler/ops et renforcer l’ouverture de session POS.

Ce fichier remplace l’ancien TODO `TODO_FIX_DRAW_OPS_AND_SESSION_AUTO.md`.

Décisions V1 à appliquer :

- Supprimer l’auto-open automatique des `SalesSession`.
- Garder uniquement l’auto-close nocturne des sessions POS oubliées.
- Supprimer l’endpoint ops draw `open-due` et garder `open-today`.
- Retirer les dépendances `features -> core.*.internal`.
- Corriger l’event cross-domain `outlet -> session`.
- Ajouter dans `OpenSalesSessionCommandHandler` une validation opérationnelle composite via un port read-only optimisé.
- Supprimer la règle “une seule session par businessDate”.
- Garder la règle “une seule session OPEN par contexte opérationnel”.

---

## 1. SalesSession auto-open — supprimer complètement

### Problème

Le scheduler actuel contient encore :

```java
@Scheduled(cron = "${tch.session.auto.open-cron:0 0 5 * * *}")
@TchJob("sales-session:auto-open")
public void tickOpen() {
    if (!canRunSalesSessionAuto("open")) return;
    commandBus.execute(new OpenDueSalesSessionsCommand());
}
```

Cette logique est à supprimer.

Une `SalesSession` représente une intention opérationnelle réelle :

```text
seller + terminal + outlet + début de travail POS
```

Elle ne doit pas être ouverte automatiquement par un batch nocturne.

### À faire

Supprimer ou déprécier :

- `SalesSessionAutoScheduler.tickOpen()`
- `OpenDueSalesSessionsCommand`
- `OpenDueSalesSessionsCommandHandler`
- tout adapter/reader utilisé uniquement par l’auto-open
- le job `sales-session:auto-open`
- les tests liés à l’auto-open automatique

### Décision métier

```text
Tchalanet ne fait pas d’auto-open automatique de SalesSession en V1.

Une SalesSession est ouverte par :
- un seller/cashier réel via le POS ;
- ou une action admin POS explicite, auditée, si cette capacité existe.

La préparation automatique de journée doit vivre dans un autre modèle si nécessaire :
OutletBusinessDay / SalesBusinessDay / POS readiness,
mais pas dans SalesSession.
```

---

## 2. YAML/properties — enlever toutes les configs auto-open

### À supprimer des fichiers YAML

Supprimer les propriétés de ce type :

```yaml
tch:
  session:
    auto:
      open-cron: "0 0 5 * * *"
```

ou :

```yaml
tch.session.auto.open-cron
```

ou toute variante :

```yaml
tch:
  session:
    auto-open:
      active: true
      cron: "..."
```

### À garder/remplacer

Garder seulement une configuration d’auto-close.

Exemple recommandé :

```yaml
tch:
  session:
    auto-close:
      active: true
      cron: "0 10 0 * * *"
      max-items-per-tenant: 1000
      close-current-business-date: false
```

### Classe properties

Renommer/adapter la classe :

Avant :

```java
SalesSessionAutoProperties
```

Après, recommandé :

```java
SalesSessionAutoCloseProperties
```

Elle ne doit plus contenir de champ `openCron`, `openActive`, `autoOpen`, etc.

Elle peut contenir :

```java
@ConfigurationProperties(prefix = "tch.session.auto-close")
public record SalesSessionAutoCloseProperties(
    boolean active,
    int maxItemsPerTenant,
    boolean closeCurrentBusinessDate
) {}
```

Le `cron` reste souvent lu directement par `@Scheduled`, donc pas forcément nécessaire dans le record.

---

## 3. SalesSession auto-close — garder et sécuriser

### Scheduler cible

Remplacer `SalesSessionAutoScheduler` par :

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class SalesSessionAutoCloseScheduler {

    private static final JobKey SALES_SESSION_AUTO_CLOSE =
        JobKey.of("session:auto-close");

    private final CommandBus commandBus;
    private final SalesSessionAutoCloseProperties properties;
    private final BatchGate gate;

    @Scheduled(cron = "${tch.session.auto-close.cron:0 10 0 * * *}")
    @TchJob("sales-session:auto-close")
    public void tickClose() {
        if (!canRun()) {
            return;
        }

        commandBus.execute(new CloseDueSalesSessionsCommand());
    }

    private boolean canRun() {
        if (!properties.active()) {
            log.info("sales_session.auto_close skipped reason=scheduler_disabled");
            return false;
        }

        if (!gate.enabled(SALES_SESSION_AUTO_CLOSE, null)) {
            log.info("sales_session.auto_close skipped reason=gate_disabled");
            return false;
        }

        return true;
    }
}
```

### Command orchestration

`CloseDueSalesSessionsCommand` peut rester sans paramètres si son handler :

- liste les tenants actifs ;
- bind un contexte tenant/system pour chaque tenant ;
- calcule `tenantToday` avec la timezone tenant ;
- cible par défaut `tenantToday.minusDays(1)` ;
- appelle une commande tenant-scoped.

Exemple :

```java
public record CloseDueSalesSessionsCommand()
    implements Command<CloseDueSalesSessionsResult> {}
```

Puis :

```java
public record CloseTenantDueSalesSessionsCommand(
    TenantId tenantId,
    LocalDate targetBusinessDate,
    Instant closedAt,
    String reason
) implements Command<CloseTenantDueSalesSessionsResult> {}
```

### Règles du handler tenant-scoped

Le handler tenant-scoped ferme uniquement :

```text
status = OPEN
business_date <= targetBusinessDate
deleted_at IS NULL
```

Il ne doit pas fermer la businessDate courante sauf option explicite.

Il doit être idempotent :

```text
si aucune session à fermer -> closedCount = 0
si déjà fermée -> pas d’erreur
```

Reason recommandé :

```text
AUTO_CLOSE_END_OF_DAY
```

Actor recommandé :

```text
SYSTEM
```

---

## 4. DrawCalendarOpsController — supprimer `open-due`

### Problème

Le controller ops contient deux endpoints d’ouverture :

```http
POST /platform/ops/draws/open-due
POST /platform/ops/draws/open-today
```

V1 doit garder un seul chemin officiel.

### À supprimer

Supprimer :

```java
@PostMapping("/open-due")
public ApiResponse<OpenDueDrawsResult> openDue(...)
```

Supprimer aussi :

```java
OpenDueDrawsCommand
```

si plus utilisé ailleurs.

### À garder

Garder :

```http
POST /platform/ops/draws/open-today
```

Décision :

```text
Draw calendar ops V1 garde uniquement open-today.

open-today ouvre les draws SCHEDULED d’aujourd’hui selon :
- drawDate tenant/channel-local
- sales_open_time atteint
- cutoff_at > now
- locked = false
```

---

## 5. DrawCalendarOpsController — retirer la dépendance `core.draw.internal`

### Problème actuel

`features.ops.draw.DrawCalendarOpsController` importe :

```java
com.tchalanet.server.core.draw.internal.infra.config.DrawProperties
```

C’est interdit.

Un module `features` ne doit pas importer `core.*.internal`.

### À faire

Retirer `DrawProperties` du controller.

Le controller ne doit pas connaître :

```java
drawProperties.getScheduler().getOpenToday().getDefaultSalesOpenTime()
```

### Option recommandée

Modifier `OpenTodayDrawsCommand` pour ne plus exiger `defaultSalesOpenTime` depuis le controller.

Controller cible :

```java
var res = commandBus.execute(new OpenTodayDrawsCommand(
    now,
    req.drawDate(),
    limit,
    req.dryRun()
));
```

Le handler `core.draw` applique les defaults internes depuis `DrawProperties`.

Si le request model permet un override ops explicite, le champ peut être optionnel :

```java
req.defaultSalesOpenTime()
```

Mais le default interne doit être résolu dans `core.draw`, pas dans `features.ops.draw`.

---

## 6. OutletDayClosedSessionListener — corriger l’event cross-domain

### Problème actuel

`core.session` importe :

```java
com.tchalanet.server.core.outlet.internal.domain.event.OutletDayClosedEvent
```

C’est interdit.

Un core ne doit pas importer l’internal d’un autre core.

### À faire

Déplacer ou exposer l’event dans :

```text
core.outlet.api.event.OutletDayClosedEvent
```

Puis modifier l’import côté session :

```java
import com.tchalanet.server.core.outlet.api.event.OutletDayClosedEvent;
```

### Listener after-commit

Remplacer :

```java
@EventListener
```

par :

```java
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
```

Exemple :

```java
@Component
@RequiredArgsConstructor
public class OutletDayClosedSessionListener {

    private final CommandBus commandBus;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOutletDayClosed(OutletDayClosedEvent event) {
        if (event.mode() == CloseDayMode.STRICT) {
            return;
        }

        commandBus.execute(
            new CloseOutletOpenSalesSessionsCommand(
                event.tenantId(),
                event.outletId(),
                event.closedDate(),
                event.occurredAt(),
                event.actorUserId(),
                "Closed by outlet day"
            )
        );
    }
}
```

### Idempotence

`CloseOutletOpenSalesSessionsCommandHandler` doit être idempotent :

```text
si les sessions sont déjà fermées -> closedCount = 0
pas d’erreur
pas de double event financier
```

---

## 7. OpenSalesSession — ajouter le port de validation opérationnelle composite

## Intention exacte

For `OpenSalesSession`, implement an optimized read-only eligibility snapshot.

The SQL adapter may join tenant, user, tenant_user/seller assignment, outlet,
terminal, calendar/closure, and sales_session tables, but it must only return facts.

Do not put business decisions in SQL.
Do not inject other core repositories into the handler.
Do not perform cross-domain writes.

The Java eligibility policy must validate the hierarchy upward:

```text
tenant -> outlet -> terminal -> seller -> session
```

Each entity must be validated both individually and relationally:

```text
- active/enabled status
- belongs to tenant
- terminal belongs to outlet
- seller belongs to tenant
- seller is allowed on outlet/terminal
- business day is open
- no current OPEN session exists for same operational context
```

---

## 7.1 Ajouter le port application

Créer dans `core.session.internal.application.port.out` :

```java
package com.tchalanet.server.core.session.internal.application.port.out;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.session.internal.application.service.opening.SalesSessionOpeningContext;
import java.time.LocalDate;

public interface SalesSessionOpeningContextReaderPort {

    SalesSessionOpeningContext loadForOpening(
        TenantId tenantId,
        OutletId outletId,
        TerminalId terminalId,
        UserId openedBy,
        LocalDate businessDate
    );
}
```


Le nom peut être ajusté, mais garder l’intention :

```text
read-only optimized opening eligibility snapshot
```

---

## 7.2 Ajouter le POJO de flags

Créer par exemple dans :

```text
core.session.internal.application.service.opening
```

```java
package com.tchalanet.server.core.session.internal.application.service.opening;

import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import java.util.Optional;

public record SalesSessionOpeningContext(
    TenantId tenantId,
    OutletId outletId,
    TerminalId terminalId,
    UserId sellerId,

    boolean tenantExists,
    boolean tenantActive,

    boolean userExists,
    boolean userActive,

    boolean sellerExistsInTenant,
    boolean sellerActiveInTenant,
    boolean sellerCanOpenPosSession,

    boolean outletExists,
    boolean outletBelongsToTenant,
    boolean outletActive,
    boolean outletBlocked,

    boolean terminalExists,
    boolean terminalBelongsToTenant,
    boolean terminalBelongsToOutlet,
    boolean terminalActive,
    boolean terminalBlocked,
    boolean terminalBound,

    boolean sellerAllowedForOutlet,
    boolean sellerAllowedForTerminal,

    boolean businessDayOpen,

    Optional<SalesSessionId> currentOpenSessionId
) {}
```

Notes :

- Le SQL peut retourner les flags.
- La décision finale reste en Java.
- Éviter un record trop “magique” avec seulement `allowed=true/false`.
- On veut savoir pourquoi c’est refusé.

---

## 7.3 Ajouter la policy Java

Créer :

```text
core.session.internal.application.service.opening.SalesSessionOpeningEligibilityPolicy
```

Exemple :

```java
package com.tchalanet.server.core.session.internal.application.service.opening;

import com.tchalanet.server.common.exception.TchConflictException;
import org.springframework.stereotype.Component;

@Component
public class SalesSessionOpeningEligibilityPolicy {

    public void requireAllowed(SalesSessionOpeningContext ctx) {
        if (!ctx.tenantExists() || !ctx.tenantActive()) {
            deny("sales.session.tenant-inactive", "Tenant is not active");
        }

        if (!ctx.userExists() || !ctx.userActive()) {
            deny("sales.session.user-inactive", "User is not active");
        }

        if (!ctx.sellerExistsInTenant() || !ctx.sellerActiveInTenant()) {
            deny("sales.session.seller-inactive", "Seller is not active for this tenant");
        }

        if (!ctx.sellerCanOpenPosSession()) {
            deny("sales.session.seller-not-allowed", "Seller cannot open POS session");
        }

        if (!ctx.outletExists() || !ctx.outletBelongsToTenant()) {
            deny("sales.session.outlet-not-found", "Outlet not found");
        }

        if (!ctx.outletActive() || ctx.outletBlocked()) {
            deny("sales.session.outlet-inactive", "Outlet is not active");
        }

        if (!ctx.terminalExists() || !ctx.terminalBelongsToTenant()) {
            deny("sales.session.terminal-not-found", "Terminal not found");
        }

        if (!ctx.terminalBelongsToOutlet()) {
            deny("sales.session.terminal-outlet-mismatch", "Terminal does not belong to outlet");
        }

        if (!ctx.terminalActive() || ctx.terminalBlocked()) {
            deny("sales.session.terminal-inactive", "Terminal is not active");
        }

        if (!ctx.terminalBound()) {
            deny("sales.session.terminal-not-bound", "Terminal is not bound");
        }

        if (!ctx.sellerAllowedForOutlet()) {
            deny("sales.session.seller-not-allowed-for-outlet", "Seller is not allowed for outlet");
        }

        if (!ctx.sellerAllowedForTerminal()) {
            deny("sales.session.seller-not-allowed-for-terminal", "Seller is not allowed for terminal");
        }

        if (!ctx.businessDayOpen()) {
            deny("sales.session.business-day-closed", "Business day is closed");
        }

        if (ctx.currentOpenSessionId().isPresent()) {
            throw new TchConflictException(
                "sales.session.already-open",
                "Sales session already open for this operational context"
            );
        }
    }

    private static void deny(String code, String message) {
        throw new TchConflictException(code, message);
    }
}
```

Le fail-fast order doit rester lisible.

---

## 7.4 Modifier `OpenSalesSessionCommandHandler`

### À supprimer du handler actuel

Supprimer :

```java
reader.findCurrentOpenByUser(command.tenantId(), command.openedBy())
```

Supprimer :

```java
reader.existsForBusinessDate(
    command.tenantId(),
    command.outletId(),
    command.openedBy(),
    businessDate
)
```

Cette règle est trop stricte :

```text
une session par user/outlet/businessDate
```

Elle bloque les e2e et le terrain après une session fermée.

### À ajouter dans le handler

Injecter :

```java
private final SalesSessionOpeningContextReaderPort openingContextReader;
private final SalesSessionOpeningEligibilityPolicy openingEligibilityPolicy;
```

Puis faire :

```java
@Override
@TchTx
public OpenSalesSessionResult handle(OpenSalesSessionCommand command) {
    var now = Instant.now(clock);

    // TODO: prefer tenant timezone from command/context/config.
    // Do not use JVM default timezone.
    var businessDate = LocalDate.now(clock);

    var openingContext =
        openingContextReader.loadForOpening(
            command.tenantId(),
            command.outletId(),
            command.terminalId(),
            command.openedBy(),
            businessDate
        );

    openingEligibilityPolicy.requireAllowed(openingContext);

    var sessionId = SalesSessionId.of(idGenerator.newUuid());

    var session =
        SalesSession.open(
            sessionId,
            command.tenantId(),
            command.outletId(),
            command.terminalId(),
            command.openedBy(),
            businessDate,
            now,
            command.openingFloatCents()
        );

    var saved = writer.save(session);

    var event =
        new SalesSessionOpenedEvent(
            EventId.of(idGenerator.newUuid()),
            now,
            command.tenantId(),
            saved.id(),
            saved.outletId(),
            saved.terminalId(),
            command.openedBy()
        );

    AfterCommit.run(() -> events.publish(event));

    return new OpenSalesSessionResult(saved.id(), now);
}
```

### BusinessDate

À améliorer si disponible :

```java
var businessDate = now.atZone(tenantZoneId).toLocalDate();
```

Ne pas utiliser une timezone implicite JVM.

---

## 7.5 Adapter SQL/JDBC read-only

Créer dans `core.session.internal.infra.persistence` :

```java
@Component
@RequiredArgsConstructor
public class JdbcSalesSessionOpeningContextAdapter
    implements SalesSessionOpeningContextReaderPort {

    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public SalesSessionOpeningContext loadForOpening(
        TenantId tenantId,
        OutletId outletId,
        TerminalId terminalId,
        UserId openedBy,
        LocalDate businessDate
    ) {
        // One optimized read-only query.
        // May join tenant, app_user, tenant_user, outlet, terminal,
        // outlet calendar/closure, assignment tables, sales_session.
        // Must only return facts/flags.
        // Must not perform writes.
        // Must not decide business rules.
    }
}
```

Important :

```text
SQL = collecte les faits
Java policy = décide
Handler = orchestre
```

Ne pas injecter les repositories internes de `core.terminal`, `core.outlet`, `core.tenantuser`, etc. dans le handler.

---

## 7.6 Contrainte DB à corriger

Supprimer ou remplacer toute contrainte de ce type :

```sql
UNIQUE (tenant_id, outlet_id, opened_by, business_date)
```

ou :

```sql
UNIQUE (tenant_id, outlet_id, terminal_id, opened_by, business_date)
```

La bonne contrainte V1 :

```sql
CREATE UNIQUE INDEX uq_sales_session_one_open_per_operational_context
ON sales_session (
  tenant_id,
  outlet_id,
  terminal_id,
  opened_by
)
WHERE status = 'OPEN'
  AND deleted_at IS NULL;
```

Règle :

```text
Une session CLOSED ne bloque pas l’ouverture d’une nouvelle session
le même businessDate.

Une seule session OPEN simultanée est autorisée par :
tenant + outlet + terminal + seller.
```

---

## 8. Tests à ajouter

### OpenSalesSession

Ajouter tests handler/policy :

- refuses inactive tenant
- refuses inactive user
- refuses seller not active in tenant
- refuses seller without POS permission
- refuses outlet not belonging to tenant
- refuses inactive/blocked outlet
- refuses terminal not belonging to tenant
- refuses terminal/outlet mismatch
- refuses inactive/blocked terminal
- refuses unbound terminal
- refuses seller not allowed for outlet
- refuses seller not allowed for terminal
- refuses closed business day
- refuses if current OPEN session exists
- allows opening if previous session exists but is CLOSED
- allows opening multiple sessions same businessDate if previous ones are CLOSED
- publishes `SalesSessionOpenedEvent` after commit

### Auto-close

- scheduler no longer has auto-open
- YAML no longer has auto-open config
- auto-close closes old OPEN sessions
- auto-close does not close current businessDate by default
- auto-close is idempotent
- auto-close uses tenant timezone

### Draw ops

- `open-due` endpoint removed
- `open-today` endpoint still works
- controller no longer imports `core.draw.internal`

### Architecture

- no `features.*` imports `core.*.internal`
- no `core.session` imports `core.outlet.internal`
- cross-domain event uses `core.outlet.api.event`
- listener uses `@TransactionalEventListener(AFTER_COMMIT)`

---

## 9. Final decisions to document

```text
core.session V1

Tchalanet ne fait pas d’auto-open automatique de SalesSession.

Tchalanet supporte un auto-close nocturne des sessions POS oubliées.
Le job auto-close est tenant-local, idempotent, gate-controlled, audité,
et ferme uniquement les sessions OPEN anciennes avec actor SYSTEM
et reason AUTO_CLOSE_END_OF_DAY.

OpenSalesSession utilise une lecture composite optimisée read-only.
Cette lecture retourne un snapshot de faits opérationnels.
La décision métier reste dans une policy Java.

Une session CLOSED ne bloque pas une nouvelle ouverture le même businessDate.

La contrainte métier V1 est :
une seule session OPEN simultanée par tenant + outlet + terminal + seller.

Aucun module features ne doit importer core.*.internal.
Aucun core ne doit importer l’internal d’un autre core.
