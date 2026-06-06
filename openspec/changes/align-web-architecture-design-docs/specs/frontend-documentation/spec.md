# Frontend Documentation Alignment

## ADDED Requirements

### Requirement: Architecture documentation distinguishes active and target structures

Web architecture documentation SHALL identify currently implemented libraries separately from
future extraction targets.

#### Scenario: Review the Web library map

- **WHEN** a contributor reads the Web architecture documentation
- **THEN** the active `ui/components`, `ui/styles`, and `ui/theme` libraries are visible
- **AND** future libraries are marked as migration targets rather than existing projects

### Requirement: Target libraries require concrete extraction

The Web project SHALL create a target Nx library only when a coherent implementation slice is moved
into it.

#### Scenario: Plan API or PageModel extraction

- **WHEN** a future change creates `api`, `page-model`, `widgets`, or another target library
- **THEN** that same change moves owned code, defines public exports, and validates dependencies

### Requirement: Shared design documentation tracks platform alignment

The central design-system documentation SHALL describe the current Web baseline and SHALL record
Mobile/POS alignment as a separate platform-owned follow-up.

#### Scenario: Read shared token guidance

- **WHEN** a contributor reads the central design-system token documentation
- **THEN** the current Web palette and token naming are documented
- **AND** Mobile/POS are not falsely described as already migrated
