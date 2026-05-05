# Tasks: mobile-flutter-distribution-v0

## 1. Décisions produit

- [x] Confirmer Android first pour vendeurs.
- [x] Confirmer iOS TestFlight seulement si client/démo l'exige.
- [x] Confirmer absence de store public en v0.
- [x] Confirmer que staging disposable n'est pas destiné à un client sans accord.

## 2. Configuration Flutter

- [x] Définir `APP_ENV`.
- [x] Définir `API_BASE_URL`.
- [x] Définir `AUTH_ISSUER`.
- [x] Définir `AUTH_CLIENT_ID`.
- [x] Choisir `dart-define` v0 ou flavors.

## 3. Build local

- [x] Documenter comment tester en local avec android studio sur mac ou avec un telephoen android.
- [x] Documenter build APK staging.
- [x] Documenter build APK prod plus tard.
- [x] Documenter versionName/versionCode.
- [x] Documenter où stocker artefacts de build.

## 4. Distribution Android

- [x] Créer stratégie APK interne court terme.
- [x] Préparer Play Console internal/closed testing plus tard.
- [x] Documenter keystore/signing sans commit de secrets.

## 5. Distribution iOS

- [x] Documenter TestFlight comme option.
- [x] Reporter Apple Business/Custom App à plus tard.

## 6. CI future

- [x] Ne pas créer workflow automatique maintenant.
- [x] Préparer brouillon `mobile-release.yml` manuel plus tard.
- [x] Ne pas lancer build mobile sur chaque PR.

## 7. Documentation

- [x] Ajouter doc `mobile-distribution-v0.md`.
- [ ] Ajouter mental model Flutter si nécessaire dans docs dev.
- [x] Documenter dépendance backend/staging/prod.
