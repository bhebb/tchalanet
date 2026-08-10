# Roadmap — version testable clients fin août 2026

## Objectif

Au plus tard le 31 août 2026, Tchalanet doit avoir une version Android
installable sur machines clients pilotes, sans installation APK manuelle, avec
un parcours vendeur crédible et des garde-fous techniques suffisants.

Cette roadmap est volontairement fonctionnelle et technique. Les procédures
d'exécution restent dans les runbooks :

- MEP : `tchalanet-infra/docs/operations/runbooks/RB-07-mep.md`
- Distribution mobile : `tchalanet-infra/docs/operations/runbooks/RB-03-mobile-distribution.md`
- Secrets : `tchalanet-infra/docs/operations/runbooks/RB-00-secrets-checklist.md`
- Release mobile : `tchalanet-mobile/docs/RELEASE.md`
- Rollback : `tchalanet-infra/docs/operations/runbooks/RB-04-release-rollback.md`
- Disaster recovery : `tchalanet-infra/docs/operations/runbooks/RB-06-disaster-recovery.md`

## Fonctionnel 1 — Vendeur / machine

Critère : un vendeur peut travailler sur une machine client sans assistance
technique.

- Login terminal/vendeur stable via Firebase.
- Bootstrap terminal/profil fiable après ouverture de l'app.
- Vente complète :
  - tirages vendables ;
  - saisie numéros ;
  - mise ergonomique après saisie des numéros ;
  - préparation ;
  - confirmation ;
  - ticket ;
  - réimpression/vérification.
- Aucune déconnexion involontaire après `prepare` ou `confirm`.
- Messages d'erreur propres, traduits, sans stacktrace ni libellé backend brut.
- Installation vendeur/client via Google Play `internal` ou `closed`, sans APK
  manuel ni explication "mode dev / sources inconnues".

## Fonctionnel 2 — Admin opérations

Critère : l'admin contrôle l'activité sans devoir vendre lui-même.

- Sidebar admin finalisée avec ordre par usage :
  - tableau de bord ;
  - vendeurs / machines ;
  - tirages ;
  - tickets ;
  - rapports ;
  - configuration repliable.
- Tirages :
  - `Tiraj ki louvri` ;
  - `Tiraj ki fèmen` ;
  - `Konfigire tiraj yo`.
- Résultats auto/manuels compréhensibles :
  - statut ;
  - source ;
  - provider ;
  - date de récupération ;
  - override manuel visible.
- Notifications utiles :
  - nouveaux résultats appliqués ;
  - résultat manuel / override ;
  - erreur provider ;
  - backup ou deploy critique.
- Limites et rapports gardés, mais rangés selon usage réel.

## Fonctionnel 3 — Surface publique / pro

Critère : aucune page vue par un client ne donne une impression brouillon.

- Pages publiques traduites au minimum en créole, français et anglais.
- Libellés cohérents entre web/admin/mobile :
  - `Maryaj gratis` ;
  - limites ;
  - tirages ;
  - machines/vendeurs ;
  - tickets.
- Cas métier visibles et expliqués proprement, par exemple :

```text
Kalifòni pa pibliye Daily4 pou tiraj midi, se poutèt sa lo2 ak lo3 pa disponib.
```

- États loading/empty/error traduits et non techniques.
- Aucun texte dur visible dans une page publique ou admin critique.

## Technique 1 — Qualité backend/web/mobile

Critère : les fonctionnalités core ne reposent plus sur des tests opportunistes.

- Backend :
  - 100% de couverture ciblée sur les services core vente, tirages, résultats,
    limites, notifications ;
  - minimum 50% global Tchalanet.
- Mobile :
  - 100% de couverture ciblée sur core vente/auth/session/API client ;
  - minimum 50% global mobile.
- Web/admin :
  - tests unitaires sur navigation/sidebar, i18n, tirages/résultats, tickets,
    limites ;
  - minimum 50% global web.
- Gates CI bloquants :
  - unit tests ;
  - integration tests ;
  - PMD ;
  - Checkstyle ;
  - SpotBugs/Spotless selon le workflow existant.

## Technique 2 — E2E et distribution mobile

Critère : une release client est reproductible sans mémoire humaine.

- E2E backend/mobile sur le flux vendeur :
  - login ;
  - sellable draws ;
  - prepare ;
  - confirm ;
  - ticket verify/reprint.
- Workflow Google Play manuel `mobile-publish-play.yml` préparé pour AAB.
- Versioning Android automatique :
  - `versionName` depuis `pubspec.yaml` ;
  - `versionCode` depuis tags `mobile/android-build-*`.
- Track Google Play `internal` prêt, puis `closed` pour les machines clients.
- Firebase App Distribution limité à QA interne.

## Technique 3 — Prod readiness / observabilité

Critère : si un pilote client casse, on sait voir, corriger et revenir arrière.

- Staging aligné prod :
  - Firebase réel ;
  - API stg ;
  - domaines terminaux cohérents.
- Grafana :
  - dashboards/alertes auth ;
  - prepare/confirm sale ;
  - résultats providers ;
  - notifications ;
  - backup ;
  - 401/403/5xx.
- Backups vérifiés et workflow backup vert.
- Runbooks MEP, rollback, secrets, distribution mobile à jour et référencés.
- SHA/tag connu-bon identifié avant chaque distribution client.

## Technique 4 — Infra prod / sécurité / opérations

Critère : la prod pilote est simple, isolée, robuste et compréhensible par les
opérations.

- Serveur prod hcloud aux USA à confirmer :
  - démarrer identique à staging pour réduire les différences ;
  - augmenter seulement si charge, Postgres ou logs le justifient ;
  - documenter région, taille, IP, volumes, firewall, backups provider.
- Secrets prod robustes :
  - nouveaux mots de passe pour DB, Redis, Edge HMAC, SMTP, provider lottery ;
  - aucune valeur staging copiée en prod ;
  - rotation initiale documentée dans Doppler `prd` ;
  - GitHub Secrets/Variables prod alignés avec RB-00.
- Checklist sécurité prod renforcée :
  - Postgres/Redis non exposés publiquement ;
  - Traefik dashboard fermé ;
  - rate-limit API/auth ;
  - SSH durci ;
  - Cloudflare DNS/TLS/WAF/cache validés ;
  - Firebase emulator/local-jwt impossibles en prod ;
  - logs sans secrets, PIN, tokens ou données sensibles.
- Service ou procédure switch-off en cas d'attaque :
  - arrêt contrôlé API/edge ;
  - page maintenance web ;
  - blocage temporaire via Cloudflare/WAF si nécessaire ;
  - canal notification interne ;
  - procédure de retour en service.
- Opérations Haïti :
  - vérifier timezone Haiti/ET sur tirages, tickets et rapports ;
  - valider connectivité mobile réaliste sur réseau instable ;
  - garder les écrans vendeurs utilisables avec peu de texte et erreurs claires ;
  - prévoir support simple pour signaler résultat/provider manquant.
- Google Play :
  - compte Play Console organisation ;
  - package `com.tchalanet.mobile` ;
  - Play App Signing ;
  - track `internal`, puis `closed` ;
  - fiche app, privacy policy, icônes/screenshots minimum ;
  - workflow AAB manuel à préparer.
- Workflows prod :
  - vérifier `deploy-infra-runtime.yml` prod ;
  - vérifier backup workflow ;
  - vérifier full validation ;
  - vérifier mobile distribution et futur publish Play ;
  - confirmer que prod ne build jamais une image différente de staging.
- Grafana free :
  - confirmer limites du plan gratuit ;
  - dashboards minimum utiles avant pilote ;
  - alertes qui ne noient pas l'équipe ;
  - logs/traces suffisants pour auth, vente, results providers, notifications.

## À confirmer avant pilote client

- Gestion des erreurs :
  - contrat code-first ;
  - traductions `ht`, `fr`, `en` ;
  - aucun message technique visible côté vendeur/admin.
- Notifications :
  - audiences admin vs vendeur ;
  - résultat auto appliqué ;
  - override manuel ;
  - erreur provider ;
  - backup/deploy critique ;
  - politique read/unread et bruit acceptable.
- Résultats providers :
  - California midi sans Daily4 expliqué proprement ;
  - source/provider/date visibles ;
  - re-fetch ou override manuel clair ;
  - surveillance des résultats attendus non arrivés.
- Archive et réconciliation :
  - archiving non encore testé à valider ;
  - reconciliation non encore testée à valider ;
  - rapport d'écart compréhensible pour admin ;
  - pas de suppression irréversible sans backup vérifié.
- Backup / restore :
  - backup automatique vert ;
  - restore testé sur environnement jetable ;
  - retention et chiffrement confirmés ;
  - premier backup prod baseline après MEP.
- Provisioning client pilote :
  - tenant ;
  - admin ;
  - vendeurs ;
  - machines/terminaux ;
  - limites ;
  - tirages actifs ;
  - compte de support.
- Données et conformité minimale :
  - privacy policy publique ;
  - conditions ou mentions basiques ;
  - contact support ;
  - politique de conservation tickets/logs à clarifier.

## Jalons

| Date cible | Résultat attendu |
|---|---|
| 10-16 août | Stabilisation vente mobile + notifications/résultats + traductions prioritaires + décisions infra prod. |
| 17-23 août | Couverture core et E2E critiques, serveur prod prêt, Doppler prod, Google Play Console/track internal prêt. |
| 24-31 août | Build pilote sur machines clients, smoke réel, backup/restore validé, corrections finales, décision GO/NO-GO. |
