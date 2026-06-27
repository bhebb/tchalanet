# Architecture batch canonique — Ops, Scheduler, Gate, Context (Spring Batch 6.0)

> **Statut** : NORMATIVE
> **Périmètre** : `tchalanet-server` (common / core / batch / features/ops / scheduler)
> **Audience** : développeurs back, reviewers, ops
> **Dernière relecture** : 2026-01-22
>
> Liens associés :
>
> - `docs/conventions/spring_batch_6.md` (obligatoire — règles SB6)
> - `docs/conventions/api/context.md` (cycle de vie `TchRequestContext`)
> - `docs/conventions/timezone.md` (Clock, ZoneId, Instant)
> - `src/main/java/com/tchalanet/server/common/cache/CACHE.md` (politique cache L1/L2)
> - `docs/flows/results_pipeline.md` (orchestration résultats / tirages)

---

## Objectif

Fournir un socle stable et normatif pour :

- les points d’entrée Ops (controllers) ;
- la couche scheduler (cron / tick) ;
- l’infra technique batch (`common.batch`) : allowlist, gate, params, binder, starter ;
- la propagation de contexte pour jobs TENANT.

## Principes clés

1. Un seul point d’entrée pour démarrer un job en production : `BatchJobStarter` ;
2. Une allowlist (JobRegistry) obligeant l’enregistrement explicite des jobs pilotables par Ops ;
3. Un gate central (BatchGate) évalué partout avant exécution ;
4. Les conversions temporelles explicites (Clock + ZoneId → Instant) ;
5. SB6 : JobOperator utilisé pour le déclenchement opérationnel.

---

## Checklist courte (à respecter)

- [ ] Tous les endpoints Ops qui déclenchent un traitement récurrent/rejouable passent par `BatchJobStarter`.
- [ ] Les actions humaines ciblées (`manual`, `override`, `confirm`, cache clear) restent directes, auditées, et n'apparaissent pas comme jobs batch.
- [ ] Tous les schedulers vérifient `BatchGate` avant d’agir.
- [ ] Les jobs TENANT installent `BatchJobExecutionListener` (binder).
- [ ] Les paramètres batch utilisent `BatchParamKeys` (snake_case constants).
- [ ] Les jobKey utilisent `BatchJobKeys` (constantes) ou `JobKey.of(...)`.
- [ ] Les conversions LocalDateTime → Instant utilisent `TimeProvider` ou `ZonedDateTime.of(..., zone).toInstant()`.

---

1. Allowlist — `JobRegistry`

---

- `JobRegistry` est la source de vérité pour les jobs pilotables par Ops.
- Un `RegisteredJob` contient : `jobKey`, `displayName`, `scope` (TENANT|GLOBAL), `requiredParams`, `optionalParams`, `springJobBeanName`.
- Les jobs non enregistrés ne doivent pas être démarrés via l’API Ops.

---

2. Controllers Ops (surface HTTP)

---

Responsabilités pour les endpoints de lancement batch :

- Valider et normaliser le `jobKey` (appel `JobKey.of(...)`).
- Vérifier la portée (TENANT vs GLOBAL) et la présence de `tenant_id` si nécessaire.
- Déléguer le démarrage à `BatchJobStarter.start(jobKey, params)`.
- Retourner une réponse orientée exécution (`executionId`, `status`, tenant ciblé) plutôt qu'un résultat métier immédiat.

Responsabilités pour les actions directes :

- Restreindre aux actions humaines courtes et ciblées sur une entité précise.
- Exiger raison/force quand l'action est sensible.
- Auditer fonctionnellement l'écriture.
- Ne jamais bypasser les invariants métier des handlers.

## Erreur / réponses

- Les erreurs de validation doivent produire des exceptions type `IllegalArgumentException` → remappées par le handler global en `ProblemDetail`.
- `BatchDisabledException` est renvoyée quand le gate bloque l’exécution.

---

3. Scheduler layer (cron / tick)

---

Règles :

- Les schedulers décident _quand_ tenter ; ils ne contiennent pas la logique métier batch ;
- Toujours vérifier `BatchGate` avant toute orchestration/trigger ;
- Les schedulers peuvent sélectionner les tenants/candidats à déclencher, mais l'exécution tenant-scoped
  se fait dans le job via `tenant_id` et le binder.

Patterns :

- Cron : pour des exécutions précises (timezone explicite). Exemple :

```java
@Scheduled(cron = "0 0 5 * * *", zone = "America/New_York")
```

- Tick (fenêtre) : pour tolérer redémarrages et dérive d’horloge :

```text
if (!gate.enabled(jobKey, null)) return;
if (!windows.isInWindow(nowLocal)) return;
trigger(...)
```

---

4. BatchWindowsConfig

---

- Définit des fenêtres d’exécution en `LocalTime` seulement ;
- C’est le scheduler qui calcule le `LocalTime` en appliquant explicitement un `ZoneId` (tenant / slot) avant d’appeler `windows.isInWindow`.

---

5. Context propagation — `BatchTchContextBinder`

---

Pourquoi : Spring Batch s’exécute hors thread HTTP → pas de `SecurityContext` ni de `TchContext`.

Règle :

- Tous les jobs TENANT DOIVENT enregistrer `BatchJobExecutionListener` qui appelle :
  - `beforeJob` → `binder.bind(jobParameters)` ;
  - `afterJob` → `binder.clear()` ;
- Le binder doit :
  - charger les informations bootstrap tenant (tenant id, tenant code, timezone, currency) ;
  - construire et publier un `TchRequestContext` dans `TchContext` (ThreadLocal) ;
  - initialiser le MDC (tenant_code, tenant_uuid, request_id) pour les logs.

Interdits dans steps :

- ❌ résoudre le tenant dans les readers/processors/writers ;
- ❌ utiliser `SecurityContextHolder` pour extraire tenant/actor ;
- ❌ dépendre du timezone JVM par défaut.

---

6. Reader / Processor / Writer rules

---

Interdits (dans ces composants) :

- logique de scheduling ;
- logique de gating ;
- résolution de tenant.

Autorisés :

- lire `JobParameters` ;
- lire le `TchContext` fourni par le binder ;
- utiliser un `Clock` / `TimeProvider` injecté pour la notion de "now".

Imports & style :

- Favoriser les `ItemReader/ItemProcessor/ItemWriter` constructeurs injection (pas de setters).

---

7. Job launching — `BatchJobStarter` (contrat concret)

---

Le projet expose `com.tchalanet.server.common.batch.launch.BatchJobStarter` (un `@Component`).

Signature canonique :

```java
public org.springframework.batch.core.job.JobExecution start(
  com.tchalanet.server.common.batch.key.JobKey jobKey,
  java.util.Map<String,String> params
)
```

Comportement (obligatoire) :

1. Valider que `jobKey` est dans `JobRegistry` (sinon IllegalArgumentException).
2. Si scope == TENANT → exiger `tenant_id` dans `params` et le parser en `TenantId`.
3. Appeler `gate.assertEnabledOrThrow(jobKey, tenantId)`.
4. Valider params via `JobParamsValidator`.
5. Construire `JobParameters` :
   - tous les params utilisateurs sont NON-identifiants (identifying=false) ;
   - `request_id` et `actor` sont remplis par défaut si manquants (non-identifiants) ;
   - `ts` est ajouté comme identifiant (epoch millis) si absent (pour éviter collision JobInstance).
6. Résoudre le bean `Job` par `registered.springJobBeanName()` via `ApplicationContext`.
7. Démarrer via `JobOperator.start(job, jobParameters)` et retourner le `JobExecution`.

Exceptions :

- `IllegalArgumentException` pour validation / params invalides ;
- `BatchDisabledException` si le gate refuse ;
- `RuntimeException` pour échecs inattendus du démarrage.

Notes :

- Les controllers convertissent le `JobExecution` en DTO API (id long, status string, startedAt Instant).
- Utiliser `BatchJobKeys` (constantes) pour éviter les littéraux de jobKey.
- Utiliser `BatchParamKeys` (constantes) pour les noms de paramètre.

---

8. Chunk size policy (NORMATIVE)

---

Principe : la taille de chunk est un paramètre technique.

Ordre de résolution pour `chunk_size` :

1. JobParameters override (ops / emergency) — uniquement si allowlisted ;
2. AppSetting (technical, GLOBAL) ;
3. `application.yml` default ;
4. fallback constant (p.ex. 10).

AppSetting (exemple) :

```yaml
tch:
  batch:
    jobs:
      draw:lifecycle:settle:
        chunk_size: 10
```

Règles :

- les overrides globaux sont autorisés ;
- overrides tenantiels sont interdits sauf justification explicite ;
- si JobParameters permet override, valider borne 1 <= chunk_size <= MAX_SAFE_CHUNK.

---

9. End‑to‑end flows (illustrations)

---

Scheduler → Batch :

1. `@Scheduled` tick ;
2. `gate.enabled` check ;
3. window check ;
4. `BatchJobStarter.start(...)` (gate.assert, démarrage via `JobOperator`, listener bind, exécution étapes, clear).

Ops → Batch :

1. HTTP POST ;
2. endpoint guide construit les params autorisés ;
3. allowlist & params validation ;
4. `BatchJobStarter.start(...)` (gate.assert inclus).

Actions directes Ops :

1. HTTP POST sur entité précise ;
2. validation + raison/force si requis ;
3. audit fonctionnel ;
4. command handler métier.

---

10. Non‑négociables (récapitulatif)

---

- `BatchGate` partout ;
- séparation claire Scheduler ≠ Batch logique ;
- Ops guidé ≠ moteur métier : quand un job existe, Ops lance Spring Batch ;
- actions directes uniquement pour opérations humaines ciblées ;
- Contexte via listener uniquement ;
- Utiliser SB6 (JobOperator pour ops) ;
- Temps = Clock + Instant (utiliser `TimeProvider`).

---

11. SB6 schema migration (rappel & procédure)

---

Le script de migration SB6 est disponible dans :
`src/main/resources/db/migration/V42__spring_batch_schema.sql`.

Principaux points :

- création du schema `batch` et des tables SB6 ;
- table `BATCH_JOB_EXECUTION_PARAMS` suivant la nouvelle structure (PARAMETER_NAME, PARAMETER_TYPE, PARAMETER_VALUE, IDENTIFYING) ;
- création des séquences `BATCH_*_SEQ`.

Procédure recommandée :

1. comparer ce script avec les sources officielles Spring Batch (repo + wiki) ;
2. tester sur snapshot DB (backup) ;
3. appliquer via Flyway en dev/staging ;
4. vérifier permissions et exécuter un job test ;
5. auditer `BATCH_JOB_EXECUTION_PARAMS` pour confirmer types/identifying flags.

---

12. TODO / améliorations recommandées

---

- Ajouter tests unitaires pour `BatchJobStarter` (happy path, invalid params, gate blocked) ;
- Ajouter intégration Testcontainers + Flyway pour valider end‑to‑end SB6 ;
- Ajouter un lint PR-check qui détecte littéraux de jobKey / param names ;
- Externaliser `TimeProvider` helpers (déjà présent) et l’utiliser partout.

---

_Fin du document._
