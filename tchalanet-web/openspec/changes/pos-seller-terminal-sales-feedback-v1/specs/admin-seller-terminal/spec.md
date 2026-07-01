# admin-seller-terminal Spec Delta

## ADDED Requirements

### Requirement: Seller terminal creation credentials

Seller terminal creation SHALL enforce a 6-character numeric PIN and guide admins toward a memorable login code.

#### Scenario: Tenant admin creates a seller terminal

- **WHEN** the admin opens the seller terminal creation form
- **THEN** the PIN field requires exactly 6 digits
- **AND** the login/terminal code field explains the format expected for a seller to log in.

### Requirement: Seller terminal creation confirmation

Successful seller terminal creation SHALL show a shell-level confirmation instead of a page-bottom notice.

#### Scenario: Seller terminal is created successfully

- **WHEN** the creation request succeeds
- **THEN** the admin sees a dismissible shell confirmation
- **AND** the seller terminal list refreshes.
