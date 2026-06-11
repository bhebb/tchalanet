# Convention — Composants Flutter partagés

**Scope** : `lib/design_system/components/`  
**Status** : normative

Les composants partagés adaptent `tchalanet-web/libs/ui/components` à Flutter et aux
contraintes POS.

## Règles

- Un composant partagé reçoit des données d'affichage et callbacks typés.
- Il ne lit ni Repository, Service, Dio, stockage ou état de feature.
- Il utilise Material 3, le thème et les tokens Tchalanet.
- Il expose les états disabled/loading/error nécessaires.
- Il ne reçoit pas une couleur arbitraire si un rôle sémantique suffit.
- Une feature ne duplique pas un bouton, badge, état vide ou panneau d'erreur déjà
  partagé.

## Familles attendues

- actions primaire, secondaire, tonal, icône et destructive ;
- loading, empty, error, offline, blocked et success ;
- badges de statut, titres de section, cartes/surfaces et erreurs de champ ;
- navigation et shell adaptatifs mobile/POS.

Un Stitch est une référence visuelle. Ses décisions réutilisables doivent devenir des
tokens ou composants partagés, pas des styles locaux copiés.

## API disponible

Importer `design_system/components/components.dart`.

| Famille | Composants |
| --- | --- |
| Actions | `PrimaryActionButton`, `SecondaryActionButton`, `TonalActionButton`, `DangerActionButton`, `SemanticIconAction` |
| Action POS | `PosActionButton` avec `PosActionButtonTone` |
| Shell/navigation | `AdaptiveNavigationShell`, `BottomActionBar` |
| Feedback | `FeedbackState` avec `FeedbackStateKind` |
| Notification transversale | `AppNotificationBanner`, rendu uniquement par `AppNotificationHost` |
| Structure | `SectionHeader`, `SurfaceCard`, `FieldError` |
| Statut | `StatusBadge`, `OnlineBadge` |

Les composants ne définissent aucun texte utilisateur. Labels, messages et tooltips
sont fournis par l'appelant après résolution i18n.

`SurfaceCardEmphasis` choisit uniquement un niveau Material 3
`surfaceContainer*`. `StatusBadgeKind` et `PosActionButtonTone` choisissent des rôles
sémantiques; aucune couleur arbitraire n'est acceptée.

`AdaptiveNavigationShell` reçoit des destinations et un callback. Il affiche une
`NavigationBar` en compact et une `NavigationRail` sur largeur medium/expanded. Il ne
connaît ni GoRouter, ni routes, ni textes de navigation.

## Erreurs et notifications

Trois canaux distincts existent :

| Canal | Exemples | Cycle de vie | UI POS |
| --- | --- | --- | --- |
| Notification interne | fermeture demain, promotion tenant, indisponibilité annoncée | `platform.notification`, persistée, read/unread/archive | centre de notifications et alerte selon sévérité |
| Actualité globale | nouvelles du secteur | persistée hors POS | jamais affichée sur le POS |
| Notice/erreur API | warning, succès partiel, erreur de la requête courante | temporaire, requête courante uniquement | `AppNotificationHost` |

- `FieldError` affiche l'erreur de validation d'un champ.
- `FeedbackState` remplace le contenu d'un écran pour un état loading, empty, error,
  offline, blocked ou success.
- `AppNotificationBanner` affiche un événement temporaire transversal au-dessus du
  layout. Les features ne l'instancient pas directement.
- `AppNotificationHost` est installé une seule fois dans `App` et consomme la file
  `appNotificationProvider`.
- Une notification transporte des clés i18n et un rôle `info`, `success`, `warning`
  ou `error`. Elle ne transporte jamais une exception brute.
- Elle peut proposer une action courte localisée, par exemple réessayer; cette action
  est consommée une fois puis ferme la notification.
- Les notifications sont temporaires et consommées une seule fois. Un blocage durable
  reste dans l'état d'écran avec `FeedbackState`.
- Une seule notification est visible. Les doublons sont ignorés, une rafale remplace
  la notification du même type encore en attente, et la file conserve au maximum trois
  éléments. Le système ne rejoue jamais une succession incontrôlée d'erreurs.
- Une erreur API peut porter une `SupportReference` avec `traceId`, `errorId`, code et
  statut HTTP. Ces détails ne sont jamais rendus dans le message utilisateur; une
  action dédiée copie la référence complète pour le support.
- `ApiResponse.notices` ne doit jamais alimenter le centre de notifications internes.
- Les notifications internes de `platform.notification` ne doivent jamais entrer dans
  la file temporaire anti-rafale; leur Repository conserve read/unread/archive.
- Le POS ne consomme pas un flux de news global. Il consomme uniquement
  `/tenant/me/notifications`.
