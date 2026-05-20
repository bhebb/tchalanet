# Gates — ArchUnit + Spring Modulith

## Spring Modulith verification

Add a verification test for application modules.
Each capability is its own module:

```text
platform.audit
platform.accesscontrol
platform.identity
platform.tenantconfig
platform.tenanttheme
platform.document
platform.communication
platform.notification
platform.idempotence
```

## ArchUnit gates

### No cross-module internal imports

No class outside a module may import:

```text
..internal..
```

from another module.

### Dependency graph

Positive allowed dependencies are preferred over broad negative rules.

Allowed macro graph:

```text
common      -> none
catalog     -> common
platform    -> common, catalog
core        -> common, catalog, platform
features    -> common, catalog, platform, core
```

Exclude framework packages from checks where needed:

```text
org.springframework..
jakarta..
java..
javax..
lombok..
io.swagger..
```

### Platform rules

- `platform` must not depend on `core` or `features`.
- Direct `platform.* -> platform.*` imports are forbidden by default.
- Platform internal packages are private.

### Core rules

- `core` must not depend on `features`.
- `core` must not listen to platform events.
- Core domain/application must not import infra.
- Core controllers must not access repositories.

### Features rules

- Features are leaf modules.
- No application module imports `features..`.
- Features do not access repositories or JPA entities.

### Completion gates

Final migration must enforce absence of:

```text
com.tchalanet.server.core.audit..
com.tchalanet.server.core.accesscontrol..
com.tchalanet.server.core.tenantuser..
com.tchalanet.server.core.tenantconfig..
com.tchalanet.server.core.tenanttheme..
com.tchalanet.server.core.notification..
```

Temporary allowlists must include:

- reason,
- owner,
- removal phase,
- TODO linked to migration task.
