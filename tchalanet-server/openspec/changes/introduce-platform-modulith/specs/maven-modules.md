# Spec — Maven Macro Modules

## Requirement: Macro Maven modules

The backend SHALL be split into six Maven modules:

- `tchalanet-common`
- `tchalanet-catalog`
- `tchalanet-platform`
- `tchalanet-core`
- `tchalanet-features`
- `tchalanet-app`

### Scenario: app assembly

Given the server is built  
When `./mvnw -pl tchalanet-app -am test` runs  
Then all required modules compile  
And only `tchalanet-app` is executable.

## Requirement: dependency direction

Maven dependencies SHALL follow:

```text
common <- catalog <- platform <- core <- features <- app
```

### Scenario: forbidden reverse dependency

Given `tchalanet-platform` needs behavior from `tchalanet-core`  
When a developer attempts to add a Maven dependency from platform to core  
Then the change is rejected  
And the behavior is moved to platform API, catalog, common, or core remains the owner.

## Requirement: no premature fine modules

The migration SHALL NOT create one Maven module per core domain or platform capability.

### Scenario: agent wants `tchalanet-core-sales`

Given a migration touches sales  
When an agent proposes a `tchalanet-core-sales` Maven module  
Then the proposal is rejected unless a separate ADR approves fine-grained physical modules.
