# Spec — Sales Fixes after Operational Context

## Required changes

- Replace ad-hoc request header validation with resolver usage.
- Remove tenant filtering from Java read paths; rely on RLS unless explicit platform/admin query.
- Ensure sales commands use typed ids only.
- Ensure sales events carry tenant/user/terminal/outlet/session metadata required by listeners.
- Ensure offline promotion uses same sell path.
