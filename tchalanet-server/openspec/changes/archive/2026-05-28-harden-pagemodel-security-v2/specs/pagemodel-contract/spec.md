# Spec — Predictable PageModel Contract

## ADDED Requirements

### Requirement: PageModel is dynamic by composition, not by shape

All rendering contracts must use stable typed schemas. Free maps are forbidden in the main rendering contract.

### Scenario: shell collections are empty
Given a private PageModel has no footer destinations
When the shell is serialized
Then `footerDestinations` is present
And its value is `[]`

### Scenario: a navigation destination appears in any surface
Given a destination appears in public, cashier, tenant admin, or superadmin shell
Then it uses the same `NavigationDestination` schema
And it has `id`, `type`, `label_key`, `label`, `path`, `icon`, `image`, `active_match`, `disabled`, `reason_key`, `badge`, and `children`

### Scenario: an image appears in any widget
Given an image appears in brand, hero, avatar, card, or feature item
Then it uses the `ImageRef` schema
And it does not use ad-hoc fields like `image_url`, `logoUrl`, or `avatarUrl`

### Requirement: Private shell separates top app bar from navigation drawer

Private top app bar must not duplicate navigation.

### Scenario: tenant admin private shell
Given a tenant admin private shell
Then `topAppBar.actions` contains utility actions only
And `navigationDrawer` contains main destinations
And top app bar contains no admin main navigation links

### Requirement: Navigation drawer follows Material-like vocabulary

The side menu contract uses a navigation drawer vocabulary.

### Scenario: navigation drawer is rendered
Given a private shell
Then `navigationDrawer.brand` contains logo/name
And `navigationDrawer.topDestinations` contains top links such as dashboard and overview
And `navigationDrawer.sections` contains grouped destinations
And `navigationDrawer.footerDestinations` contains support/release notes if any
