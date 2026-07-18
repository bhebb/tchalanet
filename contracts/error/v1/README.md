# Error Contract Fixtures V1

These fixtures are the shared wire examples for server, Angular, and Flutter contract tests. They
are data fixtures only; no runtime module may import another client family's implementation.

`ProblemDetail.title` and `ProblemDetail.detail` remain present for migration compatibility and are
non-displayable. User copy comes from `code`, `category`, approved `params`, and the local catalog.
