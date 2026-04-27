# CLAUDE.md — apps/tchalanet-mobile

> **Lire d'abord** : `../../CLAUDE.md` (règles transverses, secrets, OpenSpec)

---

## Stack mobile

| Item             | Valeur                          |
| ---------------- | ------------------------------- |
| Langage          | Dart / Flutter                  |
| State            | Riverpod                        |
| Navigation       | GoRouter                        |
| UI               | Material 3                      |
| HTTP             | Dio                             |
| Cible principale | Android — terminal POS Motorola |

---

## Règles fondamentales

- **Android-first** — optimiser pour le terminal POS Motorola avant iOS
- Pas de Cordova — Capacitor pour les APIs natives si besoin
- Riverpod pour tout le state management (pas de Provider legacy)
- GoRouter pour toute la navigation — pas de Navigator 1.0 direct
- Material 3 uniquement — pas de Cupertino sauf exception documentée
- Libs partagées : `libs/shared/`, `libs/i18n/` (fr / en / ht)
- Offline-first : le terminal POS peut être hors-ligne

---

## Skills mobile (`apps/tchalanet-mobile/.claude/skills/`)

`flutter` · `frontend-mobile`

---

## Commandes

```bash
flutter pub get              # install dépendances
flutter run                  # lancement dev (émulateur ou device)
flutter build apk --release  # build Android
flutter test                 # tests unitaires
```
