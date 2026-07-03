# platform-referentials-async-lists-v1

## Why

Platform admin referentials are spread under the existing catalog area, but their pages do not yet share the target console pattern. Lists rebuild loading/error/data state by hand and call Observable APIs directly, while the current target is data-access resources consumed by `tch-async-view`.

## What

- Confirm the platform referential inventory backed by `tchalanet-catalog`.
- Keep backend API contracts unchanged when existing catalog contracts are sufficient.
- Modernize the referential list data-access path to expose typed resources.
- Apply the new async list pattern to the main catalog referential lists.
- Align the private shell page-model navigation for platform referentials and tenant page models.
- Preserve existing CRUD actions while list loading moves to resources.

## Impact

- Platform referential list pages get stale-while-revalidate behavior and a shared error surface.
- URL-driven list state becomes the target for paginated referentials.
- Follow-up slices can apply the same recipe to settings, translations, page model templates, and tenant page model editing.

## Non-goals

- No backend API route normalization in this slice.
- No database migrations.
- No redesign of create/edit dialogs.
- No bulk CRUD generator.
- No tenant page model editor implementation in this slice.
