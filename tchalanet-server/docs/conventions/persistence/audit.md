```md
# Audit Logging (Web Annotation + Handler) — Server

## Objectif

Fournir un mécanisme standard pour auditer des actions importantes :

- via annotation `@AuditLog` (web),
- via `platform.audit.api.AuditApi` pour les écritures explicites,
  sans jamais casser le flux principal, et en respectant les transactions.

---

## 1) Ce que vous avez (base existante)

- `@AuditLog` : annotation web (entity/action + expressions)
- `AuditLogAspect` : Aspect qui intercepte les méthodes annotées
- `AuditApi` / `AuditService` : surface et service de l'audit fonctionnel
- `audit_event` : table d'audit fonctionnel

---

## 2) Règle transactionnelle (IMPORTANT)

On distingue 2 cas :

### 2.1 Cas SUCCESS (transaction commit)

- L’audit métier SHOULD être écrit **après commit** :
  - `AfterCommit.run(() -> auditApi.logAuditEvent(...))`

Pourquoi :

- si la transaction rollback, on ne veut pas loguer une action “réussie”.

### 2.2 Cas ERROR (exception / rollback)

- Si une exception est levée, il n’y aura **pas** de commit.
- Si vous voulez quand même tracer l’échec :
  - écrire l’audit **immédiatement** par la surface `AuditApi`/service dédiée,
  - avec un outcome “FAIL” dans les details.

=> Conclusion : SUCCESS = AfterCommit, ERROR = immédiat via le service d'audit.

---

## 3) Règles pour `AuditLogAspect`

### 3.1 Ne pas écrire dans les repositories depuis le web

- Le web layer ne doit pas écrire directement dans `audit_event`.
- Utiliser la surface `platform.audit.api`.

### 3.2 AfterCommit pour success

- Si `error == null` :
  - `AfterCommit.run(() -> auditApi.logAuditEvent(request))`

### 3.3 Error path

- Si `error != null` :
  - appeler immédiatement la surface d'audit avec un outcome `FAIL`

---

## 4) Expressions SpEL (rules)

Variables disponibles dans le contexte :

- paramètres de méthode : `#paramName`
- `#result`
- `#error`

Règles :

- `idExpression` doit produire un identifiant stable (String)
- `detailsExpression` peut produire un `Map` ou un objet simple

---

## 5) Détails (JSON) — règle de sérialisation

- Les détails doivent être persistables en JSONB.
- Si le résultat SpEL est déjà un `Map`, ne pas faire double conversion si inutile.
- Si c’est un objet : sérialiser via `JsonUtils` (ou `ObjectMapper`) en Map.

---

## 6) Checklist PR (audit annotation)

- [ ] endpoint annoté `@AuditLog(entity=..., action=...)`
- [ ] `idExpression` résout un id stable
- [ ] success => log AfterCommit
- [ ] error => log immédiat via la surface d'audit
- [ ] aucun accès direct aux repositories d'audit depuis le web
- [ ] aucune exception audit ne doit casser l’opération principale

---

## 7) Séparation avec Envers

`platform.audit` est l'audit fonctionnel. Il répond à “qui a fait quoi, pourquoi, depuis où ?”

L'historique technique Envers appartient à `platform.entityhistory` et lit `revinfo` + les tables
`*_aud` allowlistées. Envers ne remplace pas `audit_event`, et `audit_event` ne doit pas contenir
chaque diff de colonne.
```
