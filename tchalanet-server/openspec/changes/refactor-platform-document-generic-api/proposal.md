# Change: Refactor platform.document to a generic render contract

## Why

The current `DocumentApi` exposes specialized rendering methods such as receipt PDF, receipt ESC/POS, QR PNG and QR ESC/POS. This works for a first MVP, but it creates pressure to add domain-specific methods for every future use case: ticket receipt, payout receipt, report, notice, label, etc.

We need a single transversal contract that all modules can respect.

## What changes

- Replace specialized methods with one generic method:
  - `RenderedDocument render(DocumentRenderRequest request)`
- Introduce a stable API model under `platform.document.api.model`.
- Use typed `DocumentContent` variants to avoid forcing every caller to build a huge low-level block list.
- Keep rendering adapters under `platform.document.internal`.
- Let `sales`, `payout`, `features.pos`, and other modules build their own document requests.

## Out of scope

- Persistent document storage.
- Template management UI.
- Tenant branding registry.
- Printer fleet management.
- Full HTTP document API.
