# Haiti bounded context — tests follow-up

But: small document tracking unit & integration tests strategy for core.haiti.

Purpose
- Document test strategy for Lottery projection (domain) and Tchala catalog (infra).
- Ensure tests are pure unit tests for domain; infra tests exercise resources JSON parsing.

Scope
- Unit tests: domain (DefaultHaitiResultProjector, ExternalPick normalization, TchalaNumber)
- Application tests: query handlers (ProjectHaitiResultQueryHandler) using stubs
- Infra tests: ResourceJsonTchalaCatalogAdapter reading test resources
- Integration tests (future): wiring adapters + real DB or file systems (not in scope now)

Guidelines
- Use JUnit5 features: @Nested, @DisplayName, assertAll for grouping assertions
- Use AssertJ for fluent assertions
- Tests should avoid Spring; simple POJO instantiation
- Collections returned by domain must be immutable; assert with `assertThat(list).containsExactly...` or `isNotEmpty()`.

Files added
- tests using nested classes and display names:
  - `DefaultHaitiResultProjectorTest` (domain)
  - `TchalaNumberTest` (domain)
  - `ProjectHaitiResultQueryHandlerTest` (application)
  - `ResourceJsonTchalaCatalogAdapterTest` (infra)

Next steps
- Add more edge case tests for projector (partial mapping, missing tokens)
- Add tests for search matching multiple fields (tags, meaning)
- Add property-based tests for pick normalization (optional)

Run tests (local)

```bash
./mvnw -Dtest=DefaultHaitiResultProjectorTest,TchalaNumberTest,ProjectHaitiResultQueryHandlerTest,ResourceJsonTchalaCatalogAdapterTest test
```

Style
- Keep domain free of external libs; jackson only in infra; assertj only in tests.


