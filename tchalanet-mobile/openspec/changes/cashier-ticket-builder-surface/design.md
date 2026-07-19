# Design

## Server interaction

`POST /tenant/sales/preparations` remains mandatory before confirmation. The
bottom primary action invokes prepare while the local ticket has not yet been
verified. Its accepted response is rendered inline, including Maryaj promotion
lines and notices. The same control then invokes confirm by `preparationId`.

## Layout

The scrollable area contains the compact draw strip, game and option controls,
selection/stake entry, a thermal-ticket representation, and compact feedback.
The persistent bottom bar owns the payable total and the two commands: add line
and prepare/confirm. It uses the actual form and preparation state; no client
amount is treated as authoritative after preparation.

## Selection entry

The configured `CashierSelectionShape` remains the source of truth. Each
selection segment is represented by one numeric field with its configured digit
length. Segments are visually separated, and are serialized only when all are
complete. This prevents sellers from inferring a meaning from arbitrary
per-digit boxes.
