# Spec — GetOfflineSubmissionDetailsQueryHandler

## Responsibility

Return a detailed view of one offline submission.

## Output

`OfflineSubmissionDetails` should include:

- submission metadata;
- status;
- validation errors/reasons;
- operational identifiers;
- promotion result if promoted;
- timestamps;
- audit/admin notes if applicable.
