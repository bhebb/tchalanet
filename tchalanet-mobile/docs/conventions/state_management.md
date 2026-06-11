# Convention — Gestion d'état Riverpod

**Scope** : tout état Flutter applicatif  
**Status** : normative

Riverpod est l'unique mécanisme de gestion d'état applicatif et d'injection de
dépendances. `StatefulWidget`, `TextEditingController`, `FocusNode` et contrôleurs
d'animation restent autorisés uniquement pour l'état éphémère de présentation.

## Catégories

| Catégorie | Responsable | Durée de vie |
| --- | --- | --- |
| Focus, animation, scroll, contrôleur texte | View | widget |
| Loading, formulaire, filtre, erreur écran | ViewModel | écran/flow |
| Auth, locale, thème, connectivité | contrôleur/repository app | app/session |
| Annonces internes tenant/plateforme | Repository de notifications | persisté/session |
| Notices et erreurs API | contrôleur de notifications temporaires | requête/éphémère |
| Données, cache, offline, persistance | Repository | politique repository |
| Navigation | GoRouter | navigation |

Une View ne conserve pas une copie concurrente d'un état métier ou applicatif.

## Providers

- `Provider` : DI et valeur dérivée synchrone pure.
- `NotifierProvider` : état synchrone immuable avec commandes.
- `AsyncNotifierProvider` : état asynchrone avec commandes et lifecycle.
- `FutureProvider` : lecture simple sans commande.
- `StreamProvider` : flux externe réel exposé par un Repository.

Nouveau code : privilégier `Notifier`/`AsyncNotifier`; ne pas introduire
`StateNotifier`.

## Transitions et durée de vie

- L'état exposé est typé et immuable.
- Les commandes sont les seules mutations publiques.
- Les transitions async rendent explicites loading/success/failure.
- Les providers écran sont auto-disposés par défaut.
- Tout `keepAlive` doit être justifié et testé.
- Les providers app/session sont réinitialisés au logout et changement de tenant.
- Les états offline (`pending_sync`, `rejected`, `needs_review`, etc.) restent
  explicites.

## Persistance et effets

- Riverpod en mémoire n'est pas une persistance.
- Stockage sécurisé, DB, cache et queue offline passent par des Repositories.
- Navigation, dialogs, SnackBars, clipboard, impression et plateforme sont des effets
  one-shot consommés par la View; ils ne sont pas de l'état durable.
- Les notifications transversales temporaires passent par `appNotificationProvider`
  et sont rendues par l'unique `AppNotificationHost` racine.
- La file de notifications est bornée et dédupliquée; une rafale d'erreurs ne doit
  jamais produire une succession de messages identiques ou obsolètes.
- Les annonces internes persistantes et les notices API temporaires utilisent des
  providers et cycles de vie distincts.
- `notificationSummaryProvider` est app/session-scoped : chargement immédiat après
  login, pull du summary toutes les 30 minutes, refresh au retour au premier plan si
  nécessaire, refresh manuel forcé, reset au logout.
- `localeProvider` est app-scoped : le `LocaleRepository` est chargé avant `runApp`,
  restaure la préférence non sensible et persiste chaque changement supporté.
- Le polling ne charge jamais la liste complète. Le centre de notifications charge la
  page demandée via le Repository.
- Le ViewModel émet la décision typée; la couche UI la traduit en notification
  transversale lorsque l'effet doit survivre au `Scaffold` courant.
- Les erreurs brutes Dio/JSON ne remontent pas dans l'UI.

## Tests

Tester état initial, succès, échec, retry, refresh, disposal, restauration, reset
logout/tenant, idempotence, offline/sync et consommation unique des effets.
