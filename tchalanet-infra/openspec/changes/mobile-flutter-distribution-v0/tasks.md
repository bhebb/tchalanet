# Tasks: mobile-flutter-distribution-v0

## 1. Décisions produit

- [ ] Confirmer Android first pour vendeurs.
- [ ] Confirmer iOS TestFlight seulement si client/démo l'exige.
- [ ] Confirmer absence de store public en v0.
- [ ] Confirmer que staging disposable n'est pas destiné à un client sans accord.

## 2. Configuration Flutter

- [ ] Définir `APP_ENV`.
- [ ] Définir `API_BASE_URL`.
- [ ] Définir `AUTH_ISSUER`.
- [ ] Définir `AUTH_CLIENT_ID`.
- [ ] Choisir `dart-define` v0 ou flavors.

## 3. Build local

- [ ] Documenter comment tester en local avec android studio sur mac ou avec un telephoen android.
- [ ] Documenter build APK staging.
- [ ] Documenter build APK prod plus tard.
- [ ] Documenter versionName/versionCode.
- [ ] Documenter où stocker artefacts de build.

## 4. Distribution Android

- [ ] Créer stratégie APK interne court terme.
- [ ] Préparer Play Console internal/closed testing plus tard.
- [ ] Documenter keystore/signing sans commit de secrets.

## 5. Distribution iOS

- [ ] Documenter TestFlight comme option.
- [ ] Reporter Apple Business/Custom App à plus tard.

## 6. CI future

- [ ] Ne pas créer workflow automatique maintenant.
- [ ] Préparer brouillon `mobile-release.yml` manuel plus tard.
- [ ] Ne pas lancer build mobile sur chaque PR.

## 7. Documentation

- [ ] Ajouter doc `mobile-distribution-v0.md`.
- [ ] Ajouter mental model Flutter si nécessaire dans docs dev.
- [ ] Documenter dépendance backend/staging/prod.
