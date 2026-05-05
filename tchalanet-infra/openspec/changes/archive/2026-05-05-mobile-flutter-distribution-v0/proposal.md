# Change: mobile-flutter-distribution-v0

## Why

Tchalanet mobile migre vers Flutter. La distribution mobile ne doit pas être improvisée au moment de donner l'application à un client ou à des vendeurs. Mais elle ne doit pas non plus introduire un pipeline coûteux ou complexe avant validation produit.

## What

Définir une stratégie v0 de distribution Flutter :

```text
Phase 1 : Android pilote / APK-AAB interne
Phase 2 : iOS TestFlight si nécessaire
Phase 3 : client B2B via distribution contrôlée
Phase 4 : vendeurs via Android first, MDM/private store plus tard
```

Principes :

- Pas de build mobile automatique lourd sur chaque PR.
- `mobile-release.yml` manuel plus tard.
- Android prioritaire pour les vendeurs.
- iOS optionnel via TestFlight au début.
- Pas de store public au début.
- Environnements via flavors ou dart-defines : dev/staging/prod.

## Impact

- Réduit coût CI.
- Clarifie comment livrer une app à un client sans app store public.
- Prépare le modèle vendeurs/agents sans sur-ingénierie.

## Non-goals

- Pas de pipeline mobile complet maintenant.
- Pas de MDM en v0.
- Pas d'App Store public en v0.
- Pas de distribution vendeur massive avant pilote.
