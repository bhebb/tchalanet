# Spec: Testing Skills Review

**Author**: Claude Code  
**Date**: 2026-06-02  
**Status**: Review / Recommendation

## Context

User added testing skills:
- Global: `.agents/skills/testing-strategy/SKILL.md` (92 lines)
- Web: `tchalanet-web/.agents/skills/web-testing/SKILL.md` (62 lines)  
- Mobile: `tchalanet-mobile/.agents/skills/mobile-testing/SKILL.md` (65 lines)
- Backend: `tchalanet-server/.agents/skills/backend-testing/SKILL.md` (135 lines)

Plus 3 commands: `/unit-test-task`, `/e2e-test-task`, `/test-ready-check`

User goals:
1. **Not consume too many tokens**
2. **Have a standard for tests**

## Assessment

### Token Efficiency: GOOD with minor improvements

**Strengths:**

1. **Hierarchical loading** - Commands load `testing-strategy` (92 lines) + one slice skill (62-135 lines) = 154-227 lines per task, not all 350 lines.
2. **Slice isolation** - Web/mobile/backend skills don't duplicate each other, only add slice-specific concerns.
3. **Command-driven** - Skills are loaded only when explicitly needed via `/unit-test-task` or `/e2e-test-task`, not auto-loaded.
4. **Focused content** - Skills answer "what to test" and "when", not "how" (less verbose).
5. **Aligned with context budget** - Root `AGENTS.md` targets <500 lines outside source; 150-230 lines for testing is reasonable.

**Opportunities:**

1. **Backend skill is large** (135 lines) - covers 8 concerns: unit/controller/persistence/bus/idempotency/events/POS/Maven. Consider splitting:
   - Keep core guidance in `backend-testing/SKILL.md` (~80 lines)
   - Move specialized concerns to separate mini-skills or command-specific context:
     - `backend-testing/idempotency.md`
     - `backend-testing/pos-context.md`
     - `backend-testing/events.md`
   - Load extras only when `/unit-test-task backend core.sales idempotency` explicitly mentions them

2. **Some duplication** - "Rules" sections repeat testing best practices across skills. Consider:
   - Keep anti-patterns and principles in global `testing-strategy`
   - Keep only slice-specific rules in web/mobile/backend skills
   - Example: "fast, no network, no database" is universal → remove from backend skill

3. **Command loads could be selective** - `/unit-test-task` always loads both `testing-strategy` + slice skill. For experienced users who know the level, could skip global skill:
   - Option: `/unit-test-task --quick backend target` loads only backend skill
   - Saves 92 lines when user already knows to write unit tests

**Verdict**: Current token consumption is acceptable (150-230 lines per task). Backend skill reduction would improve to 120-180 lines.

---

### Standardization: STRONG with clarity gaps

**Strengths:**

1. **Consistent structure** - All skills follow: Purpose → Test Levels → Rules → Commands. Easy to navigate.
2. **Clear test level guidance** - `testing-strategy` answers "which level for which risk" before slice-specific "how".
3. **Architecture-aligned** - Each skill respects its stack:
   - Backend: CommandBus/QueryBus/RLS/POS operational context
   - Web: Angular/Nx/ApiResponse/PageModel
   - Mobile: Flutter/Riverpod/terminal binding/secure storage
4. **Commands enforce structure** - `/unit-test-task` requires: risk → scenarios → fixtures → dependencies → command.
5. **Pre-PR gate** - `/test-ready-check` prevents untested changes from shipping.

**Gaps:**

1. **Backend skill does too much** - Mixes unit/integration/e2e/idempotency/events/POS in one file. Hard to find relevant guidance quickly. Reader must scan 135 lines even if they only need "how to test a command handler".

2. **No negative guidance** - Skills say "do test X" but not "do NOT test Y". Missing:
   - When to skip tests (trivial getters/setters, framework code)
   - When existing coverage is sufficient (don't retest backend rules in frontend)
   - When to defer e2e (backend API e2e covers POS, mobile e2e can wait)

3. **Examples only in commands** - Skills are abstract. No concrete test file examples. Compare:
   - Current: "verify route and HTTP method"
   - Better: "verify route and HTTP method (see `OutletControllerTest.java:42`)"

4. **Anti-patterns not repeated** - Global skill lists anti-patterns, but slice skills don't reinforce slice-specific ones. Missing:
   - Backend: "Don't test private methods" → "Don't bypass CommandBus in tests"
   - Web: "Don't couple to CSS" → "Don't test Angular internals (ChangeDetectorRef)"
   - Mobile: "Don't store keys in logs" → "Don't use real printers in CI"

5. **Unclear when to NOT load** - Commands explicitly load testing skills, but nothing prevents them being loaded for non-test tasks (e.g., during `/backend-task` or `/web-task`). Should those workflows mention "do not auto-load testing skills unless adding tests"?

6. **`java-test-data` skill** - Mentioned in `/unit-test-task` but not reviewed. Is it documented? Is it necessary or could it be folded into `backend-testing`?

**Verdict**: Standardization is strong for "what to test and when". Weak on "what NOT to test" and "when is coverage sufficient".

---

## Recommendations

### Priority 1: Reduce backend skill token load

**Action**: Split `backend-testing/SKILL.md` into core + specialized concerns.

**New structure:**

```
tchalanet-server/.agents/skills/backend-testing/
  SKILL.md (80 lines)
    - Non-negotiables
    - Unit tests (handlers/domain)
    - Controller tests
    - Maven commands
  idempotency.md (30 lines)
    - Idempotency test scenarios
    - Key/payload/in-progress cases
  pos-context.md (25 lines)
    - POS operational context rules
    - Terminal/outlet/session/permission tests
  events.md (20 lines)
    - Event after-commit
    - Projector idempotency
```

**Loading rule** in `/unit-test-task`:

```markdown
Load:
1. testing-strategy
2. backend-testing/SKILL.md
3. If "idempotency" in args → backend-testing/idempotency.md
4. If "sell|payout|pos" in args → backend-testing/pos-context.md
5. If "event|listener|projector" in args → backend-testing/events.md
```

**Benefit**: Typical unit test task loads 92 + 80 = 172 lines, not 92 + 135 = 227 lines. 24% reduction.

---

### Priority 2: Add negative guidance

**Action**: Extend each skill with "Do NOT test" section.

**Global `testing-strategy`** add:

```markdown
## Do NOT test

- Framework code (Spring Boot auto-config, Angular DI, Flutter widgets).
- Trivial getters/setters with no logic.
- Private methods directly (test through public API).
- Implementation details (internal state, method call order).
- Code generated by tools (OpenAPI clients, Lombok builders).
```

**Slice-specific** add:

Backend:
```markdown
- Do not test handlers by bypassing CommandBus (use bus.execute()).
- Do not test repositories directly when RLS is the point (use integration test).
- Do not test POS flows without operational context (it's a non-negotiable).
```

Web:
```markdown
- Do not test Angular internals (ChangeDetectorRef, ViewChild).
- Do not couple tests to CSS classes unless visual behavior is the test.
- Do not retest backend business rules in frontend unit tests.
```

Mobile:
```markdown
- Do not use real printers in CI (use fake adapters or golden receipts).
- Do not store private keys in test logs.
- Do not retest backend API responses in Flutter unit tests.
```

**Benefit**: Prevents over-testing and wasted token load reading unnecessary tests.

---

### Priority 3: Clarify when to skip test skills

**Action**: Add routing hint to non-test commands.

In `/backend-task`, `/web-task`, `/mobile-task` add:

```markdown
## Context loading

Load:
1. root AGENTS.md
2. project AGENTS.md
3. scoped-task or relevant workflow skill
4. files touched by task

Do NOT load testing skills unless:
- task explicitly mentions "test" or "coverage"
- /test-ready-check is called before PR
```

**Benefit**: Prevents accidental 150-line testing context load during implementation tasks.

---

### Priority 4: Document java-test-data skill

**Action**: Either:
- Document `tchalanet-server/.agents/skills/java-test-data/SKILL.md` if it exists
- Remove reference from `/unit-test-task` if it doesn't exist
- Fold its content into `backend-testing/SKILL.md` if it's small

**Current state**: Mentioned but not found in file scan. Verify:

```bash
ls -la tchalanet-server/.agents/skills/java-test-data/
```

**Benefit**: No broken references, clearer loading rules.

---

### Optional: Add concrete examples

**Action**: Link skills to real test files.

Example in `backend-testing/SKILL.md`:

```markdown
## Controller tests

Verify:
- route and HTTP method
- @PreAuthorize security deny
- success is ApiResponse
- errors are ProblemDetail

Example: `OutletControllerTest.java:42-68`
```

**Benefit**: Reader can jump to working example instead of guessing structure.

**Cost**: Requires maintaining references as tests evolve.

---

## Impact Summary

### Projects affected
- `tchalanet-server` (backend-testing split)
- All slices (negative guidance)
- Commands (routing hints)

### Modules affected
- `.agents/skills/testing-strategy/`
- `tchalanet-server/.agents/skills/backend-testing/`
- `tchalanet-web/.agents/skills/web-testing/`
- `tchalanet-mobile/.agents/skills/mobile-testing/`
- `.claude/commands/unit-test-task.md`
- `.claude/commands/backend-task.md` (and web/mobile)

### Tasks
1. Split backend-testing skill (Priority 1) — 1 task, backend slice
2. Add negative guidance to all skills (Priority 2) — 1 task, docs
3. Update commands with routing hints (Priority 3) — 1 task, commands
4. Verify java-test-data skill (Priority 4) — 1 task, backend slice

### Risks
- **Splitting backend-testing**: must update `/unit-test-task` command to load extras conditionally. If logic is wrong, specialized guidance won't load.
- **Negative guidance**: too prescriptive = slows users; too vague = doesn't help.
- **Routing hints**: must be respected by all workflow commands, not just test commands.

### Open questions
1. Should `/test-ready-check` auto-load testing skills or rely on memory/cache from prior test tasks?
2. Should e2e tests have their own skill, or remain folded into slice skills?
3. Should integration tests (persistence/RLS/bus) be split from unit test guidance?
4. Is `java-test-data` still relevant or was it replaced by Instancio guidance in backend-testing?

### Assumptions
1. User runs test commands (<5 times per PR), not per file edit (token load is episodic).
2. Backend skill size is the bottleneck (135 lines), not global skill (92 lines).
3. Slice skills (web/mobile) are already optimal (62-65 lines).
4. Commands are the primary entry point (not free-form "write tests for X").

---

## Recommendation

### Immediate: Priority 1 + Priority 4
- Split backend-testing skill to reduce typical load from 227 to 172 lines (24% reduction).
- Verify java-test-data skill exists or remove reference.

### Next: Priority 2
- Add "Do NOT test" sections to all skills for clarity.

### Later: Priority 3
- Add routing hints to non-test commands to prevent accidental loading.

### Optional: Examples
- Link skills to real test files if references can be kept up-to-date.

---

## Conclusion

**Token efficiency**: Current setup is acceptable (150-230 lines per task). Backend skill split would improve to 120-180 lines.

**Standardization**: Strong for "what to test". Add negative guidance for "what NOT to test" and "when coverage is sufficient".

**Overall verdict**: Good foundation. Priority 1 + Priority 2 would make it excellent.
