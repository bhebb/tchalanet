# Spec delta: catalog.drawchannel

## ADDED Requirements

### Requirement: draw_channel is tenant calendar

`draw_channel` SHALL represent a tenant-scoped calendar/sales schedule for draws.

### Requirement: draw_channel references result_slot

A draw channel that depends on external results SHALL reference a global `result_slot_id`.

### Requirement: structured period

`draw_channel` SHALL expose a structured period field such as `MORNING`, `MIDDAY`, `AFTERNOON`, `EVENING`, or `LATE`.

### Requirement: display formatter belongs to drawchannel catalog

Draw channel display formatting SHALL live under `catalog.drawchannel` and be presentation-only.

#### Scenario: Display label

- GIVEN channel name `New York Midday` and draw time `14:30`
- WHEN formatter is called
- THEN it returns `New York Midday (14:30)`.

## REMOVED Requirements

### Requirement: parsing channel name for short label

Systems SHALL NOT parse the draw channel name to infer period/short label.

### Requirement: provider mapping on draw_channel

Provider external game mapping SHALL NOT live on draw_channel. It belongs to `result_slot.source_cfg`.
