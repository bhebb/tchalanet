# PR checklist — offlinesync

## Architecture

- [ ] `offlinesync` est sous `core.offlinesync`.
- [ ] Les APIs consommées par d’autres modules sont sous `api/`.
- [ ] Aucune dépendance vers `core.offlinesync.internal.*` depuis `sales`.
- [ ] Controllers thin : validation, mapping, bus, audit, response.
- [ ] Writes via `CommandBus.execute`.
- [ ] Reads via `QueryBus.ask`.

## Contexte / sécurité

- [ ] `offline grant` appelle `ctx.trustedOperationalContextRequired()`.
- [ ] `offline sync` appelle `ctx.trustedOperationalContextRequired()`.
- [ ] Validation POS faite via query dédiée.
- [ ] Pas de tenantId client comme source de vérité.
- [ ] Endpoints protégés par `@PreAuthorize` ou `@Secured`.
- [ ] Actions sensibles annotées `@AuditLog`.

## Idempotence

- [ ] Batch : même `(tenant, grant, clientBatchId)` + même hash rejoue le résultat.
- [ ] Batch : même clé + hash différent retourne conflit.
- [ ] Submission : même `clientSubmissionId` + même hash retourne `DUPLICATE` API.
- [ ] Submission : même `clientSubmissionId` + hash différent retourne conflit.
- [ ] Event consumers utilisent `ProcessedEventPort` ou équivalent.
- [ ] `promotionAttemptId` obsolète ignoré.

## Codes

- [ ] Aucun chemin ne fait `RESERVED -> AVAILABLE` après soumission.
- [ ] Code TECH_REJECTED devient `CONSUMED_REJECTED`.
- [ ] Code PROMOTED devient `CONSUMED_PROMOTED`.
- [ ] Codes non utilisés d’un ancien batch peuvent expirer/void, mais pas ceux soumis.

## Sales

- [ ] `ticket.offline_submission_id` ajouté.
- [ ] Unique `(tenant_id, offline_submission_id)` ajouté.
- [ ] Listener sales ne requête pas offlinesync.
- [ ] Events retour publiés avec `promotionAttemptId`.

## Time

- [ ] Timestamps métier en `Instant`.
- [ ] `Clock` injecté pour `now`.
- [ ] `clientSoldAt` validé contre `validFrom/validUntil`.
- [ ] `receivedAt` validé contre `syncAcceptedUntil`.

## Tests obligatoires

- [ ] Vente offline nominale promue en ticket.
- [ ] Sync après `validUntil`, avant `syncAcceptedUntil` acceptée.
- [ ] Sync après `syncAcceptedUntil` rejetée.
- [ ] Duplicate même payload sans création de ligne.
- [ ] Payload mismatch conflict.
- [ ] Event replay ne crée pas deux tickets.
- [ ] DB unique protège double ticket.
- [ ] Retour obsolète ignoré.
