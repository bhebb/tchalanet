# core-pricing Specification

## Purpose
Define tenant runtime odds ownership and effective pricing resolution keyed by technical pricing variant.
## Requirements
### Requirement: Tenant pricing is owned by core.pricing

Tenant default odds SHALL be owned by `core.pricing` as tenant runtime configuration, not by
`catalog.pricing`. The legacy catalog pricing runtime classes SHALL be removed once Java ownership
has moved; no SQL table rename SHALL be required in the first slice. `features.tenantadmin` SHALL
depend only on `core.pricing.api`, never on internal classes of another module.

#### Scenario: Tenant odds write goes through core.pricing

- **WHEN** a tenant odds change is submitted
- **THEN** it is handled by a `core.pricing.api` command
- **AND** `features.tenantadmin` does not reference `catalog.pricing` or `core.pricing` internals

### Requirement: Odds are keyed by pricing variant

Tenant default odds SHALL be keyed by `PricingVariantCode`, with functional uniqueness
`(tenant_id, game_code, pricing_variant_code)`. `bet_type` and `commercial_option_code` MAY be stored
descriptively for grouping and admin display but SHALL NOT be the pricing key. `PricingVariantCode`
SHALL be a stable enum in `core.pricing.api.model`, usable by both `core.pricing` and `core.sales`.
The runtime odds lookup SHALL be by `PricingVariantCode`, never by `(betType, betOption)`.
Seller-terminal odds overrides SHALL follow the same keying rule:
`(tenant_id, seller_terminal_id, game_code, pricing_variant_code)`. `bet_type` and `bet_option` MAY
remain descriptive bridge fields for UI/support, but SHALL NOT determine override resolution.

#### Scenario: Box variants carry different odds

- **WHEN** a Loto 4 "Permuté" is configured
- **THEN** `LOTTO4_BOX_4_WAY`, `LOTTO4_BOX_6_WAY`, `LOTTO4_BOX_12_WAY`, `LOTTO4_BOX_24_WAY` each carry their own odds
- **AND** the runtime resolves odds by the played number's resolved variant, not by the commercial option

#### Scenario: Seller-terminal override is variant-specific

- **WHEN** a seller terminal override exists for `LOTTO4_BOX_4_WAY`
- **THEN** it applies only to that pricing variant
- **AND** it does not apply to `LOTTO4_BOX_6_WAY`, `LOTTO4_BOX_12_WAY`, or `LOTTO4_BOX_24_WAY`

### Requirement: Sale snapshots the resolved variant odds; resolution and non-retroactivity preserved

Sale preparation SHALL resolve the pricing variant from the played selection and SHALL snapshot the
effective odds of that variant. Effective odds resolution SHALL remain
`seller-terminal override -> tenant default -> error`. Odds changes SHALL NOT be retroactive;
already-sold tickets SHALL settle from `TicketLine.oddsSnapshot`.

#### Scenario: Box 3-way and 6-way pay differently

- **WHEN** a Loto 3 "Permuté" line plays `112` (two identical digits)
- **THEN** the resolved variant is `LOTTO3_BOX_3_WAY`
- **AND** the snapshotted odds is the tenant odds for `LOTTO3_BOX_3_WAY`
- **AND** `123` resolves to `LOTTO3_BOX_6_WAY` with its own odds

#### Scenario: Tenant odds change affects only future sales

- **WHEN** a tenant updates a variant odds
- **THEN** future ticket lines snapshot the new odds
- **AND** previously sold tickets settle from their existing snapshot
