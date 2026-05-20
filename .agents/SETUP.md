# Claude / ChatGPT Integration — Context Setup

When asking Claude or ChatGPT to implement a backend feature, you MUST provide the relevant context.
This file tells you exactly what to copy-paste.

---

## Step 1: Decide Where Your Code Goes

Read: `tchalanet-server/openspec/context/20-backend-rules.md` (Quick Start table)

Ask yourself:

- **Has business invariants / lifecycle rules?** → `core/` (business domain)
- **Reference data only (no events, no invariants)?** → `catalog/` (read/write separation)
- **Multi-domain orchestration / workflow?** → `features/` (vertical slice)
- **Technical glue?** → `common/` (utilities, framework)

---

## Step 2: Collect & Paste the Relevant Context

### For a NEW CORE DOMAIN

Copy entire contents of these files and paste into your prompt:

```bash
cat tchalanet-server/openspec/context/20-backend-rules.md
cat tchalanet-server/docs/ARCHITECTURE.md
cat tchalanet-server/openspec/context/80-core-rules.md
cat tchalanet-server/src/main/java/com/tchalanet/server/core/<SIMILAR_DOMAIN>/DOMAIN_<SIMILAR>.md
```

Use `<SIMILAR_DOMAIN>` as a reference. For example, if building a new `payout` domain, read `sales/DOMAIN_SALES.md`.

Sample prompt:

```
I want to implement a new core domain called "reconciliation" for nightly batch reconciliation.

[paste 20-backend-rules.md]
[paste ARCHITECTURE.md]
[paste 80-core-rules.md]
[paste DOMAIN_SALES.md as reference]

Here's my requirement: ...
```

**Files Claude needs:**

- `20-backend-rules.md` — where to put code
- `ARCHITECTURE.md` § 2 — hexagonal structure
- `80-core-rules.md` — strict layer rules
- `DOMAIN_<X>.md` — reference domain example

---

### For a NEW CATALOG

Copy entire contents of these files:

```bash
cat tchalanet-server/openspec/context/20-backend-rules.md
cat tchalanet-server/docs/ARCHITECTURE.md  # sections 1 + 1.6
cat tchalanet-server/openspec/context/75-catalog-rules.md
cat tchalanet-server/docs/PLAYBOOK.md  # section 5
```

Sample prompt:

```
I want to add a new catalog module for "transaction_types" — reference data for different sale types.

[paste 20-backend-rules.md]
[paste ARCHITECTURE.md]
[paste 75-catalog-rules.md]
[paste PLAYBOOK.md section 5]

Here's my requirement: ...
```

**Files Claude needs:**

- `20-backend-rules.md` — where to put code
- `ARCHITECTURE.md` § 1 + 1.6 — catalog structure
- `75-catalog-rules.md` — read/write separation rules
- `PLAYBOOK.md` § 5 — examples

---

### For a NEW FEATURE (Orchestration/BFF)

Copy entire contents of these files:

```bash
cat tchalanet-server/openspec/context/20-backend-rules.md
cat tchalanet-server/docs/ARCHITECTURE.md  # sections 2.5
cat tchalanet-server/openspec/context/81-features-rules.md
cat tchalanet-server/docs/PLAYBOOK.md  # sections 6 + 8
```

Sample prompt:

```
I want to build a "batch_operations" feature for running nightly reconciliation workflows.

[paste 20-backend-rules.md]
[paste ARCHITECTURE.md]
[paste 81-features-rules.md]
[paste PLAYBOOK.md]

Here's the workflow: ...
```

**Files Claude needs:**

- `20-backend-rules.md` — where to put code
- `ARCHITECTURE.md` § 2.5 — feature structure
- `81-features-rules.md` — vertical slice rules
- `PLAYBOOK.md` § 6-8 — controller + inter-domain patterns

---

### For a CONTROLLER ENDPOINT (in existing domain)

Copy minimal context:

```bash
cat tchalanet-server/openspec/context/20-backend-rules.md  # Controllers section only
cat tchalanet-server/docs/PLAYBOOK.md  # section 6 (Controllers)
```

Sample prompt:

```
I need to add an endpoint to list all payouts, with pagination and filtering.

[paste 20-backend-rules.md Controllers section]
[paste PLAYBOOK.md section 6]

Here's the requirement: ...
```

**Files Claude needs:**

- Controllers pattern from `20-backend-rules.md`
- `PLAYBOOK.md` § 6 — full controller patterns with annotations

---

## Step 3: Include Your Requirement & Existing Code

Add to the same prompt:

```
My requirement:
[describe what you want to build]

Current code structure (if modifying):
[paste relevant existing code snippets]

Questions/unclear points:
[list anything you're unsure about]
```

---

## Step 4: Ask Claude/ChatGPT

Example prompts:

✅ GOOD:

```
I want to implement command to approve pending payouts.
Follow the architecture from the backend rules.

[pasted context]

Requirement: ...
```

❌ BAD:

```
Implement a payout approval command.
```

(No context → Claude guesses)

---

## Quick Reference: What Each File Contains

| File                   | Purpose                              | Size          | When To Include                |
| ---------------------- | ------------------------------------ | ------------- | ------------------------------ |
| `20-backend-rules.md`  | Layer decisions + controller pattern | 340 lines     | **ALWAYS**                     |
| `ARCHITECTURE.md`      | Full architecture overview           | 1000 lines    | **ALWAYS** (relevant sections) |
| `80-core-rules.md`     | Core domain strict rules             | 978 lines     | Core domain only               |
| `75-catalog-rules.md`  | Catalog read/write separation        | 364 lines     | Catalog only                   |
| `81-features-rules.md` | Feature vertical slice rules         | 313 lines     | Feature orchestration only     |
| `PLAYBOOK.md`          | Implementation patterns & templates  | 1086 lines    | Section 6 (Controllers) always |
| `DOMAIN_*.md`          | Business invariants & lifecycle      | 300-500 lines | Reference for similar domain   |

---

## Troubleshooting: Claude Says "I Need More Context"

If Claude asks for clarification:

1. **"How do I handle tenant isolation?"**
   → Paste: `PLAYBOOK.md` § 10 (Sécurité, Contexte & RLS)

2. **"Where do cross-domain events go?"**
   → Paste: `PLAYBOOK.md` § 8 (Inter-Domain Calls)

3. **"How do I validate input?"**
   → Paste: `PLAYBOOK.md` § 6.3 (Jakarta Bean Validation)

4. **"What's the error handling pattern?"**
   → Paste: `ARCHITECTURE.md` § 7 (Error Handling)

5. **"How do I write tests?"**
   → Paste: `PLAYBOOK.md` § 13 (Tests)

---

## Pro Tip: Create a "Context Bundle"

Save these snippets as templates you copy-paste regularly:

**`~/.context/core-domain-setup.txt`** (Mac/Linux):

```
I'm implementing a new core domain. Here's the architecture context:

[paste 20-backend-rules.md]
[paste ARCHITECTURE.md]
[paste 80-core-rules.md]

My requirement:
```

**For Windows/Other**: Use your IDE's snippet system or a text expander.

Then just:

1. Open template
2. Fill in requirement
3. Paste into Claude
4. Done ✅

---

## For Future Integration

If we add a bot/webhook:

- This file becomes the entry point
- Bot auto-loads context based on detected layer
- User provides only: requirement + existing code
- Bot handles context loading

For now, **manual copy-paste** is the workflow.
