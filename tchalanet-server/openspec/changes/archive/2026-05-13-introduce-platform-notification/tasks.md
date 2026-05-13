# Tasks

- [x] Create `platform.notification.api` package and public models.
- [x] Create `platform.notification.internal` structure.
- [x] Add `notification`, `notification_preference`, `notification_template` migrations.
- [x] Implement `NotificationApi`.
- [x] Implement `NotificationRule` and initial rules for payout/offlinesync/tenant user/batch.
- [x] Implement event router/listeners with idempotence.
- [x] Add controllers for `/tenant/me/notifications`, `/admin/notifications`, `/platform/ops/notifications`.
- [x] Add tests for read/unread/archive.
- [x] Add ArchUnit rule: no module outside platform.notification writes notification tables or imports internal package.
- [x] Document templates and targets.
