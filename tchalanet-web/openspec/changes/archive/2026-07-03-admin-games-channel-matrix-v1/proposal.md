# admin-games-channel-matrix-v1

## Why

Tenant admins need a clearer entry point for playable games and channel-specific availability. The existing draw matrix route mixes draw terminology with game setup, while Maryaj gratis already has a dedicated configuration page.

## What

- Add a tenant admin navigation section for available games.
- Keep `/app/admin/games` as the games overview route.
- Expose `/app/admin/games/channel-matrix` as the channel/game matrix route.
- Redirect legacy game matrix routes to the new canonical routes.
- Keep Maryaj gratis configuration owned by `/app/admin/maryaj-gratis`; the matrix links to that page instead of duplicating the flow.
- Use existing backend BFFs and frontend feature services for V0.

## Impact

- Web only.
- Backend PageModel navigation fragment must expose the same section as the web fallback.
- Existing admin setup links should point to the games overview.

## Non-goals

- No new backend contract in V0.
- No rewrite of Maryaj gratis configuration.
- No centralization of game-specific clients in shared API libs.
