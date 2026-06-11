# FEATURE_CONTACTREQUEST — platform.contactrequest

## Rôle

Capacité platform pour les demandes de contact public.
Possède la table, l'API de soumission publique, le CRUD admin et les notifications internes.

## Endpoints

| Méthode | Path | Accès |
|---|---|---|
| POST | `/public/contact-requests` | public |
| GET | `/platform/contact-requests` | SUPER_ADMIN |
| GET | `/platform/contact-requests/{id}` | SUPER_ADMIN |
| PATCH | `/platform/contact-requests/{id}/status` | SUPER_ADMIN |
| PATCH | `/platform/contact-requests/{id}/notes` | SUPER_ADMIN |

## Référence

Format : `CT-YY-NNNNNN` (ex. `CT-26-000042`).
Générée par `ContactRequestReferenceGenerator` via séquence PostgreSQL `contact_request_ref_seq`.
Gaps acceptés. Pas de reset annuel. Contrainte UNIQUE en DB.

## Notification

Après sauvegarde, email interne envoyé aux destinataires de `tch.public-contact.notify.recipients`.
Si l'email échoue : la demande reste créée, la réponse retourne `SUCCESS_WITH_WARNINGS`
avec notice `CONTACT_NOTIFICATION_FAILED`.

Config :
```yaml
tch:
  public-contact:
    notify:
      enabled: true
      recipients:
        - admin@tchalanet.com
      cc: []
```

## Tenant

Non-tenantée en V1 — étend `BaseEntity` (pas `BaseTenantEntity`).
`linked_tenant_id` nullable prévu pour plus tard si besoin.

## Admin web (S3 — à faire plus tard)

`features/platform/pages/contact-requests/` — voir spec dans la conversation de création.
