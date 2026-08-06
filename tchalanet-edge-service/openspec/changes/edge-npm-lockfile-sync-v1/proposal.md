# Keep the edge npm lockfile synchronized

## Why

The edge PR check passed the pnpm install but the Docker build failed at
`npm ci`. The edge `package.json` added Vite while only the pnpm lockfile was
updated, leaving `package-lock.json` inconsistent with the manifest.

## What

- Regenerate the edge `package-lock.json` from the current `package.json`.
- Verify npm installation, TypeScript build, unit tests, and the production
  Docker build.

## Impact

The edge Docker image can be built by CI with the existing `npm ci` step. The
runtime dependencies and application behavior are unchanged.

## Non-goals

- No dependency version change beyond synchronizing the existing manifest.
- No Dockerfile or workflow changes.
- No changes to notification routes or provider behavior.
