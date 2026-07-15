# Tasks

- [x] Add an infra-only core deploy script for Traefik and Redis.
- [x] Make runtime deploy require core services instead of starting them.
- [x] Replace the staging hybrid workflow with an infra-only workflow.
- [x] Rename the runtime workflow to remove infra ownership from its label.
- [x] Keep generated Traefik routers remote-owned during infra sync.
- [x] Materialize Firebase Admin credentials before API runtime deploy.
- [x] Validate workflow YAML and shell scripts.
- [x] Extract shared SSH key preparation and infra sync into reusable GitHub actions.
- [x] Rewire infra creation, full validation, and runtime deployment workflows to reuse the shared actions/workflows.
- [x] Validate workflow YAML after the reusable-action refactor.
- [x] Add disposable Neon branch support to runtime deploy workflows.
