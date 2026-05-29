# Spec — LimitPolicy impacts

## ADDED Requirements

### Requirement: SELLER scope

LimitPolicy SHALL support limits scoped to SELLER.

### Requirement: Prepaid V1 as limit

For V1, prepaid-like constraints SHALL be represented as limits if there is no financial ledger. If business needs require topup/debit/balance movement, a future SellerCredit ledger SHALL be introduced.
