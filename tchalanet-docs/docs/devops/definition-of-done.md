# Definition of Done (Tchalanet)

Appliquer à toutes les features. **Obligatoire** sauf mention contraire.

## Backend
- Endpoints et modèles **documentés** (OpenAPI/README)
- **Tests d’intégration** verts (happy path + erreurs)
- **ETag** / **Cache-Control** / **CORS** configurés
- Codes HTTP cohérents, logs d’erreurs sans fuite de données
- Variables d’env gérées (`.env.example`) et secrets non commit
- Scan basique sécurité (injections, headers) passé
- Déployé sur **staging** et validé manuellement

## Frontend Web (Angular)
- UI conforme au design, **contrastes AA**
- **Unit tests** clés + **e2e** du parcours critique verts
- **Responsive** + dark mode si prévu
- Gestion chargement/erreurs, aucune `console.error` non gérée
- **Feature flags**/règles appliqués si concernés
- **Lighthouse** OK (LCP/TBT dans nos seuils)
- Merge après review + build staging réussi

## Mobile (Ionic)
- UI fidèle iOS/Android, cibles tactiles ≥ 44px
- Auth/session gérée; offline basique si prévu
- Tests sur device/simulateur (navigation, formulaires)
- **Capacitor assets** (icône/splash) corrects
- Aucune crash/log critique; perf fluide milieu de gamme

## Commun
- Lint/format OK; pas de TODO bloquant
- **Docs** mises à jour (README/ADR/changelog)
- Accessibilité: focus-visible, labels, navigation clavier
