# Domaine <NomDuDomaine>

> Ce fichier est un **template** pour documenter un nouveau domaine backend.  
> Copie ce fichier dans `<bc>/DOMAIN.md` et remplace les sections.

---

## 1. Rôle du domaine

**Responsabilité principale**

> Décris en une phrase la responsabilité clé du domaine  
> (ex. « Calculer les permissions d’un utilisateur par tenant »).

**Ce que le domaine fait**

- Liste de 3–6 puces sur ce que le domaine couvre.

**Ce que le domaine ne fait pas**

- Liste de ce qui est explicitement hors scope (auth, audit, etc.).

---

## 2. Modèle métier (agrégats / entités)

### Entités / agrégats principaux

- `NomAggregate` — courte description.
- `AutreEntite` — description.
- `ValueObject` — description.

### Invariants métier

- Énumère les règles importantes (ex. « un ticket payé ne peut plus être annulé »).

> Valeur métier clé :  
> Résume en une phrase ce qui est “sacré” dans ce domaine.

---

## 3. Cas d’utilisation (ports d’entrée)

Interfaces côté `application.port.in` (use cases) :

- `<UseCaseName>CommandHandler` / `<UseCaseName>QueryHandler`
  - Description : …
  - Paramètres : …
  - Résultat : …

Répéter pour chaque use case important.

---

## 4. Ports de sortie (dépendances externes)

Interfaces côté `application.port.out` :

- `<Something>ReaderPort`

  - Parle à : table(s) ou service(s) externe(s) ciblés.
  - Rôle : …

- `<Something>WriterPort`
  - Parle à : …
  - Rôle : …

> Rappel : aucun autre domaine ne doit parler directement à ces tables/services.

---

## 5. Mapping & DTOs (convention)

Pour assurer une séparation claire entre les layers :

- Utiliser MapStruct pour mapper `infra.web.model` ↔ `application.command.model` / `application.query.model`.
- Créer des `Mapper` dans `infra.web.mapper` ou `infra.persistence.mapper`.
- Configurer MapStruct `componentModel = "spring"` pour autowiring.

### Audit

- Favoriser l'annotation `@AuditLog` pour instrumenter les méthodes métier critiques.
- L'aspect lié à `@AuditLog` doit construire un audit command ou utiliser `AuditEventFactory`, puis déléguer l'écriture au port d'audit.

### Records vs Lombok

- Préférer `record` pour les DTO immuables (commands / queries / simple dto).
- Si une classe nécessite JPA/Lombok, utiliser `class` + Lombok (`@Getter`, `@Builder`, etc.).

---

## 6. Règles métier importantes

- **Règle 1** : …
- **Règle 2** : …
- **Règle 3** : …

---

## 7. Intégration avec les autres domaines

Dépend de :

- Domaines en entrée (ex. Identity, AccessControl, TenantConfig).

Utilisé par :

- Domaines consommateurs (ex. Ticket, Draw, Session, etc.).

Donne un exemple de call type :

```text
someUseCase.handle(new SomeCommand(…));
```

---

## 8. Notes techniques

Packages recommandés :

- `<bc>.domain.model` → modèles métier.
- `<bc>.application.command/query` → commands, queries, handlers.
- `<bc>.application.port.in/out` → ports hexagonaux.
- `<bc>.infra.persistence` → adapters JPA.
- `<bc>.infra.web` → endpoints REST/aspects/etc.

Points d’attention :

- Multi-tenant ?
- RLS ?
- Intégrations externes ?

---

## 9. Domaines existants (référence)

À titre indicatif, les domaines actuellement présents dans Tchalanet :

- `accesscontrol` — permissions & rôles par tenant.
- `audit` — audit applicatif & révisions.
- `draw` — tirages & résultats.
- `ticket` — création & paiement de tickets.
- `session` — sessions POS & vendeurs.
- `tenantconfig` — configuration de tenant (limites, odds, etc.).
- `pagemodel` — configuration dynamique des pages publiques/privées.
- `identity` — utilisateurs & profils (hors auth Keycloak).

Mets ce nouveau domaine en cohérence avec ces patterns.
