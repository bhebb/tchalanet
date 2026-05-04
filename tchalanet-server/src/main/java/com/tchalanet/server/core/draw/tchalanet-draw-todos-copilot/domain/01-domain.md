# TODO — `core.draw` domain

## Files principaux

- `core.draw.domain.model.Draw`
- `core.draw.domain.model.DrawStatus`
- `core.draw.domain.model.DrawStatusTransition`
- `core.draw.domain.exception.*`

## P0 — corrections obligatoires

### 1. Nettoyer `Draw` constructor/factory

- [ ] Supprimer le paramètre inutilisé `DrawSource source` du constructeur `Draw`.
- [ ] Supprimer le paramètre inutilisé `String drawChannelCode` de `Draw.scheduled(...)`.
- [ ] Garder `resultSource` seulement comme metadata de résultat appliqué/corrigé.
- [ ] Ne pas utiliser `resultSource` comme clé métier.

### 2. Corriger la règle temps `cutoffAt` / `scheduledAt`

Règle métier : le cutoff arrive avant le tirage.

- [ ] Ajouter helper :

```java
private static void requireValidSchedule(Instant scheduledAt, Instant cutoffAt) {
    Objects.requireNonNull(scheduledAt, "scheduledAt is required");
    Objects.requireNonNull(cutoffAt, "cutoffAt is required");

    if (!cutoffAt.isBefore(scheduledAt)) {
        throw new IllegalArgumentException("cutoffAt must be before scheduledAt");
    }
}
```

- [ ] Appeler ce helper dans le constructeur.
- [ ] Appeler ce helper dans `reschedule(...)`.
- [ ] Remplacer la règle actuelle inversée : `scheduledAt must be before cutoffAt`.

### 3. Rendre les transitions normales strictes

Remplacer `DrawStatusTransition` par une version stricte :

```java
private static final Map<DrawStatus, Set<DrawStatus>> ALLOWED = Map.of(
    DrawStatus.SCHEDULED, Set.of(DrawStatus.OPEN, DrawStatus.CANCELED),
    DrawStatus.OPEN, Set.of(DrawStatus.CLOSED, DrawStatus.CANCELED),
    DrawStatus.CLOSED, Set.of(DrawStatus.RESULTED, DrawStatus.CANCELED),
    DrawStatus.RESULTED, Set.of(DrawStatus.SETTLED),
    DrawStatus.SETTLED, Set.of(),
    DrawStatus.CANCELED, Set.of()
);
```

Si `ARCHIVED` existe :

```java
DrawStatus.SETTLED, Set.of(DrawStatus.ARCHIVED),
DrawStatus.CANCELED, Set.of(DrawStatus.ARCHIVED),
DrawStatus.ARCHIVED, Set.of()
```

- [ ] Retirer `SETTLED -> RESULTED` des transitions normales.
- [ ] Retirer `CANCELED -> SCHEDULED`.
- [ ] Retirer `CANCELED -> OPEN`.
- [ ] Retirer `RESULTED -> CANCELED` du cycle normal sauf décision contraire avec guard sales + audit.
- [ ] Ajouter validation null dans `check(from, to)`.

### 4. Restreindre `applyResult(...)`

- [ ] `applyResult(...)` doit être uniquement `CLOSED -> RESULTED`.
- [ ] Refuser si `drawResultId` est déjà présent.
- [ ] Garder la correction dans `overrideResult(...)` / `CorrectAppliedDrawResultCommandHandler`.

Exemple cible :

```java
public void applyResult(DrawResultId resultId, Instant now, DrawSource resultSource) {
    ensureNotLocked();
    DrawStatusTransition.check(this.status, DrawStatus.RESULTED);

    if (this.drawResultId != null) {
        throw new DrawInvalidResultException(id, "Draw already has a result");
    }

    this.drawResultId = Objects.requireNonNull(resultId, "resultId is required");
    this.status = DrawStatus.RESULTED;
    this.resultedAt = Objects.requireNonNull(now, "now is required");
    this.resultSource = Objects.requireNonNull(resultSource, "resultSource is required");
}
```

### 5. Restreindre `overrideResult(...)`

- [ ] `overrideResult(...)` doit être limité à `status == RESULTED`.
- [ ] Ne pas permettre override sur `SCHEDULED`, `OPEN`, `CLOSED`, `CANCELED`, `SETTLED` sans commande dédiée.
- [ ] Reason obligatoire et trim.
- [ ] Ne pas changer `status` si déjà `RESULTED` ; garder `RESULTED`.

### 6. Restreindre `reschedule(...)`

Recommandation MVP stricte :

- [ ] Autoriser `reschedule(...)` seulement si `status == SCHEDULED`.
- [ ] Refuser `OPEN`, `CLOSED`, `RESULTED`, `SETTLED`, `CANCELED`.
- [ ] Si besoin futur de reschedule OPEN, créer commande admin auditée spécifique.

### 7. `archive()`

- [ ] Appeler `ensureNotLocked()` dans `archive()` sauf décision explicite inverse.
- [ ] Autoriser archive seulement depuis états terminaux si `ARCHIVED` est un status.

### 8. Nettoyage

- [ ] Supprimer `requireText(...)` inutilisé ou l'utiliser dans `cancel/override`.
- [ ] Remplacer les `IllegalStateException` importantes par exceptions domaine plus explicites si possible.
- [ ] Vérifier orthographe unique : `CANCELED` vs `CANCELLED`.

## P1 — hardening

- [ ] Ajouter tests unitaires de transitions.
- [ ] Ajouter tests `cutoffAt < scheduledAt`.
- [ ] Ajouter tests lock guard.
- [ ] Ajouter tests `applyResult` refuse résultat déjà présent.
- [ ] Ajouter tests `overrideResult` refuse draw non RESULTED.

## Définition de terminé

- Aucun retour arrière dangereux dans `DrawStatusTransition`.
- Aucune correction admin cachée dans les transitions normales.
- Le domaine ne connaît ni provider, ni draw channel code, ni result slot code, ni timezone.
