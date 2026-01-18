# Testing Rules

- JUnit 5
- AssertJ ONLY
- Group assertions with assertAll(...)
- Use @Nested for scenarios
- Prefer in-memory ports over mocks
- Test names: should*<expected>\_when*<condition>

# Clarification

- Method names MUST be Java-compatible (camelCase). Use `@DisplayName("should <expected> when <condition>")` on the test method to provide the human-readable pattern (this is the canonical test description shown in reports).
- Example:

```java
@Nested
@DisplayName("When URL lang is provided")
class WhenUrlLangProvided {

  @Test
  @DisplayName("should use URL lang when allowed")
  void shouldUseUrlLangWhenAllowed() {
    // given
    var input = "...";

    // when
    var res = resolve(input);

    // then
    assertThat(res).isEqualTo("fr");
  }
}
```
