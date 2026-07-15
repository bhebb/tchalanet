# RB-04 — Release smoke & rollback

Runbook de déploiement serveur V0 : smoke post-deploy, décision de rollback,
rollback applicatif, rollback base de données et clear cache.

À lire avec :
- [`../DEPLOYMENT.md`](../DEPLOYMENT.md) — mécanique de déploiement (Make targets).
- [`../IMAGES-DEPLOYMENT.md`](../IMAGES-DEPLOYMENT.md) — publication d'images GHCR.
- [`RB-01-staging-provision.md`](./RB-01-staging-provision.md) — provisioning serveur.
- [`../OPERATIONS.md`](../OPERATIONS.md) — backups et opérations courantes.

Tracking : issue #250.

---

## 1. Principes

- **Déploiement par tag d'image.** L'API et l'edge tournent depuis des images
  GHCR épinglées par `IMAGE_TAG` (voir `scripts/docker/publish-images.sh` qui
  écrit `IMAGE_TAG`/`API_IMAGE_BASE` dans `envs/common/compose.env`).
  **Rollback applicatif = redéployer le tag connu-bon précédent.**
- **Migrations forward-only (Flyway), expand-contract.** Les migrations V0 sont
  **non destructives** (ajout de colonnes/tables, jamais de suppression
  destructive en même temps que le changement de code). Conséquence : redéployer
  une image précédente est **sûr** tant que la migration appliquée est additive —
  l'ancien code ignore les colonnes nouvelles.
- **Restore DB = uniquement** si une migration destructive/ratée a été appliquée
  ou si les données sont corrompues. C'est l'option de dernier recours.
- **Toujours prendre un backup DB avant un déploiement qui applique une
  migration** (voir §3).

---

## 2. Pré-deploy — à faire AVANT

- [ ] CI verte sur le commit à déployer : `server-pr.yml` (`mvnw -DskipITs=true verify`).
- [ ] Noter le **tag connu-bon actuel** (pour rollback rapide) :
  ```bash
  make ssh-staging   # ou ssh-prod
  grep '^IMAGE_TAG=' /opt/tchalanet-infra/envs/common/compose.env
  ```
  → consigner la valeur (ex. `IMAGE_TAG=stg-20260713-abc123`).
- [ ] Si la release applique une migration : **backup DB** (§3).
- [ ] Vérifier que la nouvelle migration est additive (pas de `DROP`/`ALTER … DROP`
  destructif couplé au même déploiement).

---

## 3. Backup base de données

**Staging** (depuis le repo infra, poste opérateur) :
```bash
tchalanet-infra/scripts/remote/staging-backup.sh
# → pg_dumpall gzippé dans backups/staging/staging-pg-<timestamp>.sql.gz
```

**Prod** : même principe (`pg_dumpall`/`pg_dump -Fc` de `tchalanet_db`, voir
`OPERATIONS.md`). ⚠️ Le script `staging-backup.sh` cible `stg-app` et la clé
`~/.ssh/tchalanet_stg` ; pour prod, adapter le serveur (`prod-app`) et la clé
(`~/.ssh/tchalanet_prod`) — **à paramétrer/valider avant le premier go-live prod**.

---

## 4. Déploiement

Voir `DEPLOYMENT.md`. En résumé :
```bash
make deploy-staging     # push infra + ssh + env-merge + up-staging
# ou
make deploy-prod        # idem sur prod-app
```
`up-staging`/`up-prod` font `env-merge` + `render-traefik` + certs + réseaux, puis
lèvent la stack (Traefik, Postgres, Redis, API, edge, Firebase auth externe).
Flyway applique les migrations au démarrage de l'API.

---

## 5. Smoke post-deploy

Smoke automatisé :
```bash
make smoke-staging      # scripts/utils/smoke-staging.sh
```

Smoke manuel minimal (checklist #250) :

- [ ] **Health** : `GET /actuator/health` → `UP`
  ```bash
  curl -sf https://api.stg.tchalanet.com/actuator/health
  ```
- [ ] **Observabilité** : une requête renvoie un `X-Request-Id` / trace, et une
  erreur volontaire (404) renvoie un `ProblemDetail` avec `requestId`/`traceId`
  sans fuite sensible.
- [ ] **Login admin** OK (tenant-admin).
- [ ] **Login SellerTerminal** OK.
- [ ] **Vente ticket test** OK (POS sell, idempotency-key acceptée).
- [ ] **Print / reprint** identiques.
- [ ] **Dashboard / reporting minimal** joignable selon rôle.
- [ ] **Batch / ops endpoint** accessible selon permissions (SUPER_ADMIN).

Si un item échoue → passer à §6.

---

## 6. Décider : rollback ou fix-forward ?

| Situation | Action |
|---|---|
| API ne démarre pas / health KO | **Rollback applicatif** (§7) vers le tag connu-bon |
| Régression fonctionnelle sans corruption de données | **Rollback applicatif** (§7) |
| Migration additive OK mais bug applicatif | Rollback applicatif (§7) — la DB additive reste compatible |
| Migration destructive/ratée, données incohérentes | **Rollback DB** (§8) + rollback applicatif |
| Changement de format cache (payload) | **Clear cache** (§9) — souvent suffisant, sinon rollback |
| Vente bloquée en prod | §10 en priorité |

Règle : privilégier le **rollback applicatif** (rapide, sûr avec migrations
additives). Ne toucher à la DB (§8) que si la migration est en cause.

---

## 7. Rollback applicatif (redéployer le tag précédent)

1. Récupérer le **tag connu-bon** noté en §2.
2. Sur le serveur cible, épingler `IMAGE_TAG` à ce tag et redéployer l'API (+ edge) :
   ```bash
   make ssh-staging     # ou ssh-prod
   cd /opt/tchalanet-infra
   # repositionner le tag connu-bon
   sed -i 's/^IMAGE_TAG=.*/IMAGE_TAG=<tag-connu-bon>/' envs/common/compose.env
   # redéployer les services runtime depuis GHCR
   IMAGE_TAG=<tag-connu-bon> ./scripts/remote/deploy-runtime-services.sh
   ```
   (`deploy-runtime-services.sh` lit `API_IMAGE_TAG`/`EDGE_IMAGE_TAG`/`IMAGE_TAG`.)
3. Re-lancer le smoke (§5). Health `UP` + vente test = rollback réussi.

> Note : le tag connu-bon doit toujours exister sur GHCR (ne pas purger les
> images N-1). Vérifier avec `docker manifest inspect ghcr.io/<org>/tchalanet-api:<tag>`.

---

## 8. Rollback base de données (dernier recours)

À utiliser **uniquement** si une migration destructive/ratée est en cause.

**Staging** :
```bash
tchalanet-infra/scripts/remote/staging-restore-latest.sh
# restaure le dernier backups/staging/staging-pg-*.sql.gz (demande confirmation,
# écrase la base via `psql -U postgres` sur le conteneur postgres)
```

**Prod** : le script `staging-restore-latest.sh` est **codé pour staging**
(`stg-app` + `~/.ssh/tchalanet_stg`). Pour prod, il faut un équivalent paramétré
(serveur `prod-app`, clé `~/.ssh/tchalanet_prod`) — **à créer/valider avant
go-live prod**. Ne pas improviser une restore prod à la main sous incident.

Après restore : redéployer le tag applicatif compatible avec le snapshot DB
restauré (généralement le tag connu-bon, §7), puis smoke (§5).

---

## 9. Clear cache

Si un changement modifie le format d'un payload caché (Redis) :

- Endpoint Ops (SUPER_ADMIN) — `CacheOpsController` (`features/ops/cache`) :
  list / clear by name / clear all, sous préfixe `/api/v1`.
- Redis est une **optimisation** (l'app fonctionne Redis off) ; en cas de doute,
  vider le cache concerné est sûr.
- Les writes admin évictent déjà leurs caches via `@CacheEvict`.

---

## 10. Vente bloquée — triage prioritaire

1. **Health** API (`/actuator/health`) et logs (chercher le `requestId` de la
   requête de vente en échec).
2. Vérifier Postgres joignable et migrations appliquées (l'API log Flyway au
   démarrage).
3. Si l'incident suit un déploiement → **rollback applicatif (§7)** immédiat.
4. Redis : tester avec Redis désactivé si suspicion cache.
5. Escalade : `<à compléter — contact/astreinte>`.

> TODO ops : renseigner les contacts d'astreinte et le canal d'incident.

---

## 11. Post-incident

- [ ] Consigner : tag déployé, tag de rollback, cause, migration en jeu.
- [ ] Si rollback DB : vérifier l'écart de données (ventes créées entre backup et
  incident) et décider du rejeu éventuel.
- [ ] Ouvrir un correctif fix-forward et re-tester avant nouveau déploiement.
