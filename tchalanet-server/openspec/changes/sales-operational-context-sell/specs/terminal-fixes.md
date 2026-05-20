# Spec — Terminal API/Fixes for Operational Context

## Requirement

`core.terminal` SHALL expose a stable API/query to verify terminal eligibility.

## Needed contract

`GetTerminalOperationalEligibilityQuery` or equivalent returns:

- terminal id;
- outlet id;
- active status;
- tenant association if explicit;
- terminal type/capabilities if relevant.

## Rules

- Terminal/outlet mismatch must fail before sales mutation.
- No other module imports terminal persistence.
