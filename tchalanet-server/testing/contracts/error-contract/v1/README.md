# Error Contract Fixtures v1

These fixtures are shared examples for Java, Angular, and Flutter tests.

They describe the product contract, not a full runtime snapshot:

- blocking failures are `ProblemDetail` JSON with a stable `code`;
- successful degraded responses are `ApiResponse<T>` JSON with stable notices/services;
- `title`, `detail`, and `message` are diagnostic compatibility fields, not product copy;
- clients translate by `code` and use correlation fields only for support diagnostics.

Dynamic fields such as `timestamp`, `requestId`, `traceId`, `spanId`, and `errorId` use fixed sample
values so tests can parse the shape deterministically.
