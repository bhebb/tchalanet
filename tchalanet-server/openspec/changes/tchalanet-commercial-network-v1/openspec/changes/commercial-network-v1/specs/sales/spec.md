# Spec — Sales impacts

## MODIFIED Requirements

### Requirement: Seller snapshot on ticket

Sales SHALL snapshot seller details at ticket creation: sold_by_user_id, seller_id, seller_assignment_id, seller display/code snapshot when useful, seller commission policy snapshot.

### Requirement: Transactional seller revalidation

Sales SHALL not rely only on a stale seller resolution. Before committing a sale, Sales SHALL revalidate seller ACTIVE, assignment active/open, assignment matches outlet, and assignment valid at soldAt.

### Requirement: Promotion line fields

TicketLine SHALL support origin, pricingSource, selectionSource, payoutBaseAmount, promotionDecisionId nullable, oddsOverride nullable.

### Requirement: Charge snapshots

Sales SHALL snapshot applied and waived charges.
