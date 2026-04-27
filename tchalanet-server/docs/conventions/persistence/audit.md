```md
# Audit Logging (Web Annotation + Handler) — Server

## Objectif

Fournir un mécanisme standard pour auditer des actions importantes :

- via annotation `@AuditLog` (web),
- via use-case `LogAuditEventCommand` (core.audit),
  sans jamais casser le flux principal, et en respectant les transactions.

---

## 1) Ce que vous avez (base existante)

- `@AuditLog` : annotation web (entity/action + expressions)
- `AuditLogAspect` : Aspect qui intercepte les méthodes annotées
- `AuditLoggingCommandHandler` : writer robuste (REQUIRES_NEW)
- `LogAuditEventCommand` : modèle de commande
- `AuditEventWriterPort` + `AuditEventFactory`

---

## 2) Règle transactionnelle (IMPORTANT)

On distingue 2 cas :

### 2.1 Cas SUCCESS (transaction commit)

- L’audit métier SHOULD être écrit **après commit** :
  - `AfterCommit.run(() -> commandBus.send(new LogAuditEventCommand(...)))`

Pourquoi :

- si la transaction rollback, on ne veut pas loguer une action “réussie”.

### 2.2 Cas ERROR (exception / rollback)

- Si une exception est levée, il n’y aura **pas** de commit.
- Si vous voulez quand même tracer l’échec :
  - écrire l’audit **immédiatement** en `REQUIRES_NEW` (ce que fait votre handler),
  - avec un outcome “FAIL” dans les details.

=> Conclusion : SUCCESS = AfterCommit, ERROR = immédiat (REQUIRES_NEW).

---

## 3) Ajustements recommandés à votre `AuditLogAspect`

### 3.1 Ne pas appeler le handler directement

- Le web layer ne doit pas invoquer un handler “use-case” directement.
- Utiliser le **CommandBus** (canonique).

✅ au lieu de :
`handler.handle(new LogAuditEventCommand(...))`

✅ faire :
`commandBus.send(new LogAuditEventCommand(...))`

### 3.2 AfterCommit pour success

- Si `error == null` :
  - `AfterCommit.run(() -> commandBus.send(cmd))`

### 3.3 Error path

- Si `error != null` :
  - appeler immédiatement `commandBus.send(cmd)` (le handler étant REQUIRES_NEW)

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
- [ ] error => log en REQUIRES_NEW (sans casser le flux)
- [ ] appel via CommandBus (pas direct handler)
- [ ] aucune exception audit ne doit casser l’opération principale
```
