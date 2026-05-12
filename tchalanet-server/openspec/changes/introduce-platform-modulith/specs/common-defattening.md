# Spec — Common Defattening

## Requirement: common contains only technical shared kernel code

`common` SHALL NOT contain application workflows, persistence-owning business services, or tenant-specific behavior.

### Scenario: communication helper sends Slack messages

Given a class in common routes messages to Slack/email/SMS  
When it owns delivery decisions, templates, retries or persistence  
Then it MUST move to `platform.communication`.

### Scenario: PDF helper formats bytes only

Given a stateless low-level PDF utility with no tenant/business workflow  
When it is reused by document generation  
Then it MAY remain in common  
But document generation workflow moves to `platform.document`.

### Scenario: idempotency owns records

Given idempotency stores request records and manages replay behavior  
When it has persistence or workflow  
Then the service moves to `platform.idempotence`  
And only low-level annotations/interfaces may remain in common.

### Scenario: security permission decision

Given a class decides whether a user has a domain/application permission  
When the decision depends on roles, assignments or tenant policies  
Then it belongs in `platform.accesscontrol`  
And common keeps only Spring Security glue.
