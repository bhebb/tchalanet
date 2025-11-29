Domaine Audit

1. Rôle du domaine

Responsabilité principale

Fournir une traçabilité complète, fiable et multi-tenant des actions effectuées dans la plateforme Tchalanet.

Ce que le domaine fait

Capture les événements d’audit applicatifs (actions métier).

Enregistre qui a fait quoi, sur quelle entité, quand, depuis où.

Assure la cohérence multi-tenant (chaque événement appartient à un tenant).

Fournit l’historique des modifications des entités (révisions Envers).

Expose une API métier simple :

« loggue cet événement d’audit »

« donne-moi les derniers événements »

« donne-moi les révisions d’une entité »

Ce que le domaine ne fait pas

Authentification (c’est Keycloak).

Permissions / Access control (c’est AccessControl).

Feature flags (Unleash).

Agrégation de logs techniques système.

Notification / analytics.

Archivage externe (S3/Glacier → hors périmètre du domaine core).

2. Modèle métier (agrégats / entités)
   2.1. Agrégat principal : AuditEvent

Un événement d’audit représente une action métier concrète, contextualisée :

acteur → qui ?

entité cible → quoi ?

action → quoi a été fait ?

tenant → dans quel contexte ?

paramètres → détails JSON optionnels.

2.2. Entités / Value Objects

AuditEvent
Contient l’ensemble des informations relatives à une action métier (voir § invariants).

AuditAction
Enum couvrant les actions génériques : CREATE, DELETE, PAY, LOGIN, etc.

AuditEntityType
Enum décrivant le type d’entité métier cible (tenant, ticket, draw, terminal…).

AuditActorType
Enum : USER, SYSTEM, TERMINAL.

EntityRevision
Value object représentant une révision Envers d’une entité (historique DB).

2.3. Invariants métier

Un AuditEvent doit avoir :

un entityType + entityId valides,

un action non null,

un actorType cohérent avec actorId,

un detailsJson toujours valide JSON ({} minimal).

tenantId reflète toujours le tenant courant lorsqu’il existe.

Les événements doivent être immutables après persistance.

La sérialisation des détails ne doit jamais casser un audit (fallback {}).

Valeur métier clé : un événement d’audit est une trace durable,
indépendante des tables métier, et doit être interprétable dans 1 an.

3. Cas d’utilisation (ports d’entrée)

Interfaces côté application.port.in (use cases exposés aux autres domaines).

3.1. RecordAuditEventCommandHandler

Description : enregistre un événement d’audit pour une action métier.

Paramètres : AuditEntityType, entityId, AuditAction, Map<String,Object> details.

Résultat : aucun (fire & forget).

Comportement :

Récupère le contexte utilisateur (tenantId, userId, ip, userAgent).

Construit un AuditEvent valide.

Persiste via AuditEventWriterPort.

3.2. ListRecentAuditEventsQueryHandler

Description : retourne les derniers événements d’audit d’un tenant.

Paramètres : tenantId, limit.

Résultat : liste d’AuditEvent.

Utilités :

UI admin / super admin,

monitoring interne,

diagnostic d’activité.

3.3. PurgeOldAuditEventsCommandHandler

Description : supprime les événements trop anciens (rétention).

Paramètres : aucun (retention-days via config).

Résultat : aucun.

Utilité : gestion de la taille des tables.

3.4. ListEntityRevisionsQueryHandler

Description : retourne l’historique des révisions (Envers) d’une entité métier.

Paramètres : tenantId, entityType, entityId, limit.

Résultat : liste d’EntityRevision.

4. Ports de sortie (dépendances externes)

Interfaces côté application.port.out.

4.1. AuditEventWriterPort

Rôle : persister les événements d’audit.

Méthodes :

save(AuditEvent event)

deleteBefore(Instant threshold) (purge)

4.2. AuditEventReaderPort

Rôle : lecture optimisée des événements d’audit.

Méthode :

findRecentForTenant(UUID tenantId, int limit)

4.3. RevisionReaderPort

Rôle : lecture des révisions Envers.

Méthode :

findRevisions(tenantId, entityType, entityId, limit)

Aucun autre domaine n’accède directement aux tables audit_event ou revinfo.
Le domaine Audit est le seul « gateway » pour la traçabilité.

5. Événements de domaine

Pas de publication explicite en V1.

Idées pour V2 :

AuditEventRecorded
→ permet d’alimenter un bus interne, analytics, alertes.

EntityRevised
→ émission après révision Envers.

6. Règles métier importantes

Règle 1 : un audit appartient toujours à un tenant (RLS obligatoire).

Règle 2 : actorType définit explicitement la nature de l’acteur (user / système / terminal).

Règle 3 : les détails doivent être sérialisables en JSON sans erreur.

Règle 4 : l’audit ne doit jamais bloquer le métier :

si sérialisation impossible → fallback {}.

si audit down → log warning mais ne bloquer pas la transaction métier.

Règle 5 : l’aspect @AuditLog n’implémente aucune logique métier :

il traduit seulement une méthode → en RecordAuditEventCommand.

7. Intégration avec les autres domaines

Audit dépend :

de common.context (pour le TchRequestContext),

du stockage multi-tenant (RLS PostgreSQL).

Audit est utilisé par :

tous les domaines métier (ticket, draw, session, tenantconfig),

le domaine AccessControl (ex : login/logout, override tenant),

l’infrastructure (purge, monitoring).

Pattern d’appel typique :

recordAuditEvent.handle(
new RecordAuditEventCommand(
AuditEntityType.TICKET,
ticketId,
AuditAction.CREATE,
Map.of("amount", 25)
)
);

Exemple avec annotation :

@AuditLog(entity = TICKET, action = CREATE, idExpression = "#result.id")
public Ticket createTicket(...) { ... }

8. Notes techniques

Packages recommandés :

audit.domain.model → AuditEvent, EntityRevision, enums.

audit.application.command → commands + handlers Record/Purge.

audit.application.query → queries + handlers ListRecent/ListRevisions.

audit.application.port.in/out → ports hexagonaux.

audit.infra.persistence → JPA (audit_event) + Envers (revinfo).

audit.infra.web → annotation @AuditLog + aspect.

Points d’attention :

Toujours filtrer par tenant_id.

Adapter JPA doit mapper strictement les enums en texte stable (pas ordinal).

Les champs sensibles (IP, userAgent) doivent être correctement typés (inet, text).

Le type jsonb doit toujours recevoir une chaîne JSON valide.

Le job de purge ne doit jamais supprimer des événements d’autres tenants (sécurité RLS).
