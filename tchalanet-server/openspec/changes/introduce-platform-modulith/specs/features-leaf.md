# Spec — Features as Leaf Modules

## Requirement: features expose HTTP contracts only

Features SHALL NOT expose Java APIs by default.

### Scenario: core imports a feature model

Given `core` imports `features.tenantadmin.*`  
When architecture tests run  
Then the build fails.

## Requirement: reusable logic is not shared from features

If behavior is reused by multiple features or modules, it SHALL move to common, catalog, platform or core depending on responsibility.

### Scenario: two features share tenant admin assembly

Given two features need the same tenant profile data  
When the logic is not UI-specific  
Then it moves to `platform.identity.api` or the owning core/catalog API  
And not to `features.shared` unless it is strictly UI-only.

## Requirement: features may consume public APIs

Features MAY consume:

- common primitives;
- catalog APIs;
- platform APIs;
- core commands/queries/events/models.

They MUST NOT consume internal packages or repositories.
