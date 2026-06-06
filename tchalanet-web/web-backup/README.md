# Web Backup Migration Reference

This directory contains only Web sources that still need review or migration.

Already recovered sources are removed instead of maintained in parallel:

- runtime theme: `libs/ui/theme`;
- reusable shell/navigation/error components: `libs/ui/components`;
- active public/private shells;
- shared SCSS primitives already promoted to `libs/ui/styles`.

Remaining pages, widgets, assets, and style primitives are references only. Migrate them through a
scoped change, adapt them to the active contracts and architecture, then remove their backup source.
