# Design — theme-preset-deletion-policy

## Vue d'ensemble

Ce design implémente une politique de fallback automatique lorsqu'un `ThemePreset` devient indisponible, garantissant qu'aucun tenant ne reste sans thème fonctionnel.

---

## Décisions architecturales

### DA1 — Pas de cascade destructrice

**Décision** : Quand un preset est retiré, on ne touche PAS aux `tenant_theme` existants.

**Rationale** :

- Audit trail : on garde la trace de ce que le tenant avait configuré
- Réversibilité : si le preset est réactivé, la config tenant est intacte
- Simplicité : pas de job batch de nettoyage, pas de risque de casser des tenants en masse

**Implication** :

- La résolution du thème effectif doit gérer le cas "preset référencé mais indisponible"
- Le fallback est appliqué à la volée (runtime), pas en base

---

### DA2 — Cascade de fallback explicite

**Décision** : Ordre de fallback normé :

1. Tenant default (si `tenant.default_theme_preset_code` configuré)
2. Platform default (preset avec flag `is_default=true` ou code conventionnel `default-light`)
3. Hardcoded safe preset (`default-light` en dur)

**Rationale** :

- Prévisible : les admins savent quel preset sera appliqué
- Flexible : permet une config tenant-specific ou globale
- Safe : dernier recours toujours disponible

**Implication** :

- Nécessite un service dédié `TenantThemeFallbackService`
- Nécessite une convention pour le preset "safe" (hardcoded)

---

### DA3 — Warning notice obligatoire

**Décision** : Émettre un warning structuré lors de chaque fallback appliqué.

**Rationale** :

- Observabilité : les ops doivent savoir quand un fallback est déclenché
- Debug : facilite la remédiation (identifier les tenants impactés)
- Metrics : permet de mesurer l'impact d'un retrait de preset

**Format** :

```java
public record TenantThemeNotice(
  String code,              // "THEME_PRESET_UNAVAILABLE_FALLBACK_APPLIED"
  TenantId tenantId,
  String requestedPresetCode,
  String fallbackPresetCode,
  Instant timestamp
) {}
```

**Émission** :

- Via logger structuré (JSON) avec marker `TENANT_THEME_FALLBACK`
- OU via event Spring `ApplicationEventPublisher` (si on veut des listeners)

---

### DA4 — Suppression hard delete

**Décision** : Le service admin n'expose plus de hard delete pour les presets.

**Rationale** :

- Intégrité : évite de casser les références `tenant_theme.preset_code`
- Audit : les presets retirés restent visibles dans l'historique
- Réversibilité : un preset peut être réactivé (`active=true`, `deleted_at=NULL`)

**API admin** :

- `softDelete(ThemePresetId)` : set `deleted_at=now()` + `active=false`
- `deactivate(ThemePresetId)` : set `active=false` seulement (dépublication temporaire)
- `reactivate(ThemePresetId)` : set `active=true` + `deleted_at=NULL` (optionnel, future)

---

## Composants impactés

### 1. `catalog/theme` (admin service)

**Changements** :

- Supprimer `hardDelete` si existe
- Assurer que `softDelete` fait bien `deleted_at + active=false`
- Ajouter `deactivate(ThemePresetId)` si absent

**Responsabilité** :

- Gestion du cycle de vie des presets
- Pas de logique de fallback (c'est le rôle de `core/tenanttheme`)

---

### 2. `core/tenanttheme` (fallback service)

**Nouveau composant** : `TenantThemeFallbackService`

**Responsabilité** :

- Résoudre le preset de fallback selon la cascade définie
- Pas d'accès direct à la base : délègue à `ThemeCatalog` et `TenantConfigPort`

**Interface** :

```java
public interface TenantThemeFallbackService {
  String resolveFallback(TenantId tenantId, String unavailablePresetCode);
}
```

**Implémentation** :

```java
@Service
@RequiredArgsConstructor
public class TenantThemeFallbackServiceImpl implements TenantThemeFallbackService {

  private final ThemeCatalog themeCatalog;
  private final TenantConfigPort tenantConfig; // pour lire tenant.default_theme_preset_code

  @Override
  public String resolveFallback(TenantId tenantId, String unavailablePresetCode) {
    // 1. Tenant default
    var tenantDefault = tenantConfig.getDefaultThemePresetCode(tenantId);
    if (tenantDefault.isPresent() && isAvailable(tenantDefault.get())) {
      return tenantDefault.get();
    }

    // 2. Platform default
    var platformDefault = themeCatalog.findByCode("default-light"); // ou flag is_default
    if (platformDefault.isPresent() && platformDefault.get().active()) {
      return platformDefault.get().code();
    }

    // 3. Hardcoded safe
    return "default-light";
  }

  private boolean isAvailable(String code) {
    return themeCatalog.findByCode(code)
      .filter(ThemePresetView::active)
      .isPresent();
  }
}
```

---

### 3. `core/tenanttheme` (query handler)

**Changements** : `ResolveTenantThemeQueryHandler`

**Logique** :

```java
@Override
public TenantThemeView handle(ResolveTenantThemeQuery query) {
  var tenantTheme = readerPort.findByTenantId(query.tenantId());

  if (tenantTheme.isEmpty()) {
    // Pas de thème configuré pour ce tenant → fallback immédiat
    var fallbackCode = fallbackService.resolveFallback(query.tenantId(), null);
    emitNotice(query.tenantId(), null, fallbackCode);
    return buildViewFromPresetCode(query.tenantId(), fallbackCode);
  }

  var requestedCode = tenantTheme.get().presetCode();
  var preset = themeCatalog.findByCode(requestedCode);

  if (preset.isEmpty() || !preset.get().active()) {
    // Preset indisponible → fallback
    var fallbackCode = fallbackService.resolveFallback(query.tenantId(), requestedCode);
    emitNotice(query.tenantId(), requestedCode, fallbackCode);
    return buildViewFromPresetCode(query.tenantId(), fallbackCode);
  }

  // Preset disponible → retourner tel quel
  return tenantTheme.map(t -> new TenantThemeView(...)).get();
}

private void emitNotice(TenantId tenantId, String requested, String fallback) {
  var notice = new TenantThemeNotice(
    "THEME_PRESET_UNAVAILABLE_FALLBACK_APPLIED",
    tenantId,
    requested,
    fallback,
    Instant.now(clock)
  );
  // Logger structuré ou event publisher
  logger.warn("Theme fallback applied", structuredLog(notice));
}
```

---

## Flow diagrams (ASCII)

### Scénario 1 : Preset actif (happy path)

```
Tenant A → ResolveTenantThemeQuery
  → tenant_theme.preset_code = "dark-v1"
  → ThemeCatalog.findByCode("dark-v1") → found + active=true
  → Return TenantThemeView(preset="dark-v1")
```

---

### Scénario 2 : Preset inactif (fallback)

```
Tenant A → ResolveTenantThemeQuery
  → tenant_theme.preset_code = "old-preset"
  → ThemeCatalog.findByCode("old-preset") → found BUT active=false
  → FallbackService.resolveFallback(A, "old-preset")
     → Check tenant default → not set
     → Check platform default "default-light" → found + active
     → Return "default-light"
  → Emit notice THEME_PRESET_UNAVAILABLE_FALLBACK_APPLIED
  → Return TenantThemeView(preset="default-light")
```

---

### Scénario 3 : Preset soft-deleted

```
Tenant A → ResolveTenantThemeQuery
  → tenant_theme.preset_code = "archived"
  → ThemeCatalog.findByCode("archived") → Optional.empty (filtered by deleted_at)
  → FallbackService.resolveFallback(A, "archived")
     → ... (same cascade)
  → Return TenantThemeView with fallback
```

---

## Impacts base de données

**Aucune modification de schéma requise.**

- `theme_preset` : déjà supporte `active` + `deleted_at`
- `tenant_theme` : reste inchangé (pas de cascade FK)

**Migration SQL** : Aucune (politique applicative).

---

## Impacts API

**Aucune modification d'API publique.**

- `GET /tenant/theme` continue de fonctionner
- Le fallback est transparent pour le client (il reçoit toujours un thème valide)

---

## Observabilité

### Logs structurés

Format recommandé :

```json
{
  "level": "WARN",
  "marker": "TENANT_THEME_FALLBACK",
  "message": "Theme preset unavailable, fallback applied",
  "tenantId": "123e4567-e89b-12d3-a456-426614174000",
  "requestedPresetCode": "old-preset",
  "fallbackPresetCode": "default-light",
  "timestamp": "2026-01-23T18:30:00Z"
}
```

### Métriques (optionnel)

- Counter `tenant_theme_fallback_total{requested_preset, fallback_preset}`
- Permet de mesurer l'impact d'un retrait de preset

---

## Sécurité

**Aucune implication sécurité directe.**

- Le fallback ne change pas les permissions
- Le tenant voit toujours un thème conforme à ses droits

---

## Performance

**Impact négligeable** :

- Un appel supplémentaire à `ThemeCatalog.findByCode` en cas de fallback
- La cascade de fallback est courte (max 3 lookups)
- Les reads `ThemeCatalog` sont cachés (déjà existant)

---

## Migration / Rollout

**Pas de migration de données.**

**Rollout** :

1. Déployer le code avec fallback
2. Vérifier que les presets `default-light` (ou équivalent) sont actifs
3. Désactiver progressivement les presets obsolètes
4. Monitorer les notices de fallback

**Rollback** :

- Si problème, réactiver les presets retirés (`active=true`)
- Le fallback ne casse rien (lecture seule)

---

## Alternatives considérées et rejetées

### Alt 1 : Cascade delete automatique de tenant_theme

**Rejeté** : perte d'historique, casse les tenants sans avertissement.

### Alt 2 : Migration batch automatique lors du retrait

**Rejeté** : complexe, risque d'erreurs, nécessite stratégie de rollback. Peut être ajouté plus tard en opt-in.

### Alt 3 : Bloquer le retrait si des tenants référencent le preset

**Rejeté** : empêche la dépublication rapide d'un preset, freine l'évolution produit.

---

## Future enhancements (hors scope)

- Admin UI pour "migrer tous les tenants de preset X vers Y"
- Job batch de nettoyage périodique (optionnel)
- Flag `tenant_theme.preset_override` pour forcer un preset même si indisponible (edge case)
