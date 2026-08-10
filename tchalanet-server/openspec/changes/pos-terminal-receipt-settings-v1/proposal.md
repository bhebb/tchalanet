# POS terminal receipt settings

## Why

The mobile POS needs a terminal-scoped policy for quick sale and receipt output. The policy must be generic across Sunmi, Bluetooth ESC/POS, and manual PDF fallback without storing physical pairing details on the server.

## Scope

- Add terminal-scoped quick-sale and receipt printer settings.
- Expose and update them through the existing POS profile contract.
- Keep printer pairing and device capabilities local to the app installation.

Post-sale delivery channels (SMS, WhatsApp, email) are outside this change and belong to `post-sale-delivery-channels-v1`.
