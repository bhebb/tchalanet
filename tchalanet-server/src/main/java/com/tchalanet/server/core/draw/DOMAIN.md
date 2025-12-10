Domaine Draw

1. Rôle du domaine

Responsabilité principale

Gérer le cycle de vie des tirages (draws) pour les différents canaux de jeu de la plateforme Tchalanet.

Ce que le domaine fait

Crée et planifie les futurs tirages.

Gère les statuts des tirages (ex: SCHEDULED, OPEN, CLOSED, CANCELED).

Fournit des informations sur les tirages à venir et passés.

Ce que le domaine ne fait pas

Gestion des tickets de jeu (c'est le domaine `ticket`).

Gestion des paiements (c'est le domaine `payout`).

Configuration des canaux de jeu (c'est le domaine `tenantconfig`).

2. Modèle métier (agrégats / entités)
   2.1. Agrégat principal : Draw

Un tirage représente un événement de jeu planifié, avec un statut et des dates clés.

2.2. Entités / Value Objects

Draw
Contient l'ensemble des informations relatives à un tirage.

DrawStatus
Enum couvrant les différents statuts d'un tirage : SCHEDULED, OPEN, CLOSED, CANCELED.

DrawChannel
Représente un canal de jeu pour lequel un tirage est organisé.

2.3. Invariants métier

Un Draw doit avoir :

un `channelCode` valide,

une `scheduledDate` dans le futur lors de sa création,

un statut non nul.

Les dates de `cutoff` doivent être cohérentes avec la date du tirage.

Un tirage ne peut être modifié que sous certaines conditions de statut.

3. Cas d’utilisation (ports d’entrée)

Interfaces côté application.port.in (use cases exposés aux autres domaines).

3.1. CreateDrawCommandHandler

Description : planifie un nouveau tirage pour un canal.

Paramètres : `channelCode`, `scheduledDate`.

Résultat : l'ID du nouveau tirage.

3.2. ListUpcomingDrawsQueryHandler

Description : retourne les prochains tirages pour un canal.

Paramètres : `channelCode`, `limit`.

Résultat : liste de `Draw`.

3.3. CancelDrawCommandHandler

Description : annule un tirage planifié.

Paramètres : `drawId`.

Résultat : aucun (fire & forget).

4. Ports de sortie (dépendances externes)

Interfaces côté application.port.out.

4.1. DrawWriterPort

Rôle : persister les tirages.

Méthodes :

`save(Draw draw)`

4.2. DrawReaderPort

Rôle : lecture optimisée des tirages.

Méthode :

`findById(UUID drawId)`

`findUpcomingByChannel(String channelCode, int limit)`

5. Événements de domaine

Idées pour V2 :

DrawCreated
→ pour notifier d'autres systèmes de la planification d'un nouveau tirage.

DrawStatusChanged
→ pour déclencher des actions en fonction du changement de statut d'un tirage.

6. Règles métier importantes

Règle 1 : un tirage appartient toujours à un tenant (RLS obligatoire).

Règle 2 : les statuts des tirages suivent un cycle de vie strict.

Règle 3 : les opérations sur les tirages sont idempotentes.

7. Intégration avec les autres domaines

Draw dépend :

de `common.context` (pour le TchRequestContext),

du stockage multi-tenant (RLS PostgreSQL).

Draw est utilisé par :

le domaine `ticket` pour la vente de tickets,

le domaine `payout` pour le calcul des gains.

8. Notes techniques

Packages recommandés :

`draw.domain.model` → Draw, DrawStatus, enums.

`draw.application.command` → commands + handlers Create/Cancel.

`draw.application.query` → queries + handlers ListUpcoming.

`draw.application.port.in/out` → ports hexagonaux.

`draw.infra.persistence` → JPA (draw).

`draw.infra.web` → controllers REST.
