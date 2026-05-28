# Spec: PageModel template seeding

## Requirement: templates seed before instances

PageModelTemplate rows must be loaded before PageModel instances are onboarded.

### Scenario: startup seed order
Given the application starts
When PageModelTemplateSeedRunner runs
Then it seeds catalog templates from static files
And PageModelOnboardingRunner runs after it

## Requirement: onboarding uses template catalog as source

The onboarding service must iterate default/system templates from catalog.

### Scenario: default tenant onboarding
Given catalog contains default system templates
When onboarding runs for the default tenant
Then one published PageModel instance is created for each missing template

## Requirement: logical_id remains stable identity

The PageModel logical id must remain the stable key across template and instance.

### Scenario: seed superadmin dashboard
Given a template with logical_id `private.dashboard.superadmin`
When onboarding creates a PageModel instance
Then the instance logicalId is `private.dashboard.superadmin`
And scope and slug come from the template columns

## Requirement: template metadata is not runtime UI contract

Technical template fields are allowed in catalog seed files but must not be treated as public/back-office renderer fields.

### Scenario: runtime PageModel response
Given a PageModel was created from a template
When frontend requests the PageModel
Then the runtime model contains the renderer contract
And seed-only fields such as `is_system` are not required by the Angular renderer
