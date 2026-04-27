# Feature Notifications (BFF)

> BFF pour exposer les notifications utilisateur (tenant) et gérer leur accusé de réception.

> Functional overview (MkDocs): `tchalanet-docs/docs/02-functional/features/notifications.md`

---

## 1. Rôle & objectifs

- Lister les notifications pour l’utilisateur courant.
- Marquer comme lues/ack.

---

## 2. Endpoints (tenant)

- GET `/tenant/notifications` — liste paginée.
- POST `/tenant/notifications/{id}:ack` — accusé réception.

Retour: `ApiResponse<TchPage<NotificationResponse>>` / `ApiResponse<AckResponse>`.

---

## 3. Handlers appelés & agrégation

- Queries: `ListUserNotificationsQuery`.
- Commands: `AckNotificationCommand`.

---

## 4. Pagination & cache

- `@TchPaging TchPageRequest` pour list.
- Cache L1 optionnel (ttl court) pour listing.

---

## 5. Sécurité & permissions

- `@Secured` (roles tenant).
- Context via `@CurrentContext` (user/tenant).

---

## 6. Notes techniques

- DTO suffixes; wrappers ID.
- Pas de logique métier.
