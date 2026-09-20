# Trails design inspiration from Mobbin

The Mobbin observations below were inspected September 14, 2026. Updated September 15 to distinguish those historical references from the current [Trails design and completion contract](trails-completion-contract.md) and [implementation plan](superpowers/plans/2026-09-14-trails-v25-store6-completion.md). The v2.5 checkpoint is already committed; Store6 and full UX acceptance remain required.

## Current Trails direction

[Trails v3 in Figma](https://www.figma.com/design/4B7GK9ndPVQ1BFIKGg0Zqj/Trails?node-id=153-97) is the visual authority. The user allowed a custom theme and inspiration from apps such as Airbnb and TikTok, requested Apple-guided refinement, and restored the original compass. HeroUI informs the Figma components; implementation uses Compose and TrailsTheme. Explore, Saved, Activity and Profile are the planned roots. Moments, maps and recording are included in the current Figma contract. Exact reproduction of an AllTrails collection is no longer the completion boundary.

## Source scope

The [original AllTrails iOS collection](https://mobbin.com/apps/all-trails-ios-66ac61f7-f9ea-4c45-b512-b2fadf7e027b/caf6da24-bc01-47b9-a571-0f4db9d55dfe/screens) is retained as the original inspiration source, not the implementation authority. Mobbin now exposes working screen and flow searches. The results identify apps, screens, and flows, but do not identify their collection/version in the returned metadata. These inspected AllTrails references therefore establish useful visual evidence without establishing complete coverage of that exact collection.

Screen descriptions below come from inspected images. Flow observations cover only the preview positions explicitly listed. Still captures establish visible states, not animation timing or runtime correctness. Exact font families, color values, spacing measurements, and missing flow steps remain to be verified. Canonical Mobbin links are retained because returned image links expire.

## AllTrails patterns to use

| Reference | What is visible | Direction for Trails |
| --- | --- | --- |
| [Explore with route imagery](https://mobbin.com/screens/ff4665a2-2e2f-4fb6-b486-acc6b39fde69) | Search at the top, Nearby/Kid-friendly/Dog-friendly chips, a large satellite-route card, rating/difficulty/distance, Preview and Trail details actions, and a floating green Map button. Bottom labels read Explore, Community, Navigate, Saved, Profile. | Make trail discovery the main content. Keep search and quick filters close to results, and make the list/map relationship visible. These are AllTrails labels; Trails uses the roots in its Figma contract. |
| [Trail details and saved feedback](https://mobbin.com/screens/fc4f4c20-dc64-4ee6-b5f9-a3f093e30db7) | A large waterfall photograph, circular actions over the image, a rounded white details surface, title and rating, and a two-column facts grid for length, elevation gain, estimated time, and loop type. A dark confirmation reads Saved to Favorites with Edit. | Lead with the place, then make effort and suitability easy to scan. Keep the same trail facts and save state across discovery, detail, and saved views. |
| [Saved list](https://mobbin.com/screens/ff1be8a8-814d-44b7-b677-1f7512db2094) | A named list, a photo card with route thumbnail and trail facts, an invitation to explore more trails, and bottom Download and Map actions. | Give saved hikes a useful destination with clear follow-on actions. Saving an item and downloading map content must have distinct states. |
| [Custom route planning](https://mobbin.com/screens/fbb9b733-a59c-4c53-9447-4122f82bfe61) | A map with a route, Tap/Draw control, a lower Custom Route sheet, compact metrics, an elevation profile, and a green Save action. | If custom route planning is later selected for Trails, give the map most of the space and use the sheet for route facts and the next action. Treat route editing as its own feature and state contract. |
| [Route preview](https://mobbin.com/screens/f1d76efb-b7e0-407b-972e-9d8519ca2ab8) | Full-screen satellite imagery with a highlighted route and position marker, a length/elevation overlay, and a bottom trail label with a pause control. | Keep the map unobstructed and controls compact. This capture alone does not prove live GPS recording or background navigation behavior. |

The shared visual language in these captures is outdoor photography and cartography, white rounded surfaces, dark text, compact metadata, circular image controls, and bright green primary actions. The current Figma design adapts these ideas through the Trails brand. Its tokens/components and recorded native adaptations govern implementation; these captures do not override them.

## Observed flow previews

### Saving a trail

[Saving a trail to a list](https://mobbin.com/flows/56a52c85-e97f-44b0-9c95-e44a5248cbe7) contains four screens. Mobbin rendered positions 1, 3, and 4; position 2 was returned as metadata but was not visually inspected.

- Position 1 shows trail details with an unfilled bookmark and bottom Download/Map actions.
- Position 3 shows a Save to a list sheet over the dimmed detail screen. It offers Create new list, existing lists with checkboxes, and a Save button.
- Position 4 shows the detail screen with a filled bookmark and Saved to Favorites feedback with Edit.

For Trails, use this visible structure for the first complete Store6 journey as defined in the current completion contract: discover a trail, inspect it, save it, and find it in the saved destination. The sheet is reusable feature UI; the trail and saved destinations remain screens. Local save success and remote acknowledgement must remain distinct. An offline save can persist immediately while a separate, unobtrusive state communicates pending synchronization.

### Searching and changing views

[Searching AllTrails](https://mobbin.com/flows/8a88978b-f5f0-4040-afb0-653f41883930) contains six screens. Mobbin rendered positions 1, 4, and 6; positions 2, 3, and 5 were not visually inspected.

- Position 1 shows photo results under search and quick-filter chips, with a floating Map action.
- Position 4 shows a Los Angeles query over a map, filter chips, clustered markers, map controls, and a lower sheet headed 223 trails.
- Position 6 shows the same location query above satellite-route cards and a floating map control.

For Trails, preserve one query and stable trail identities across list, map, and detail. Returning from details should restore the chosen results and position. These captures show filter entry points and chips; they do not establish the unseen difficulty/length filter controls or the exact intervening gestures.

## Optional adjacent inspiration

These references provide comparison ideas; the current Trails Figma contract remains authoritative.

| Reference | Observed pattern | Possible use |
| --- | --- | --- |
| [komoot route summary](https://mobbin.com/screens/fb7d0cc3-88c6-4f68-a50c-f5b21a8b1fd5) | Editable sport, pace, direction, and waypoints above duration, distance, uphill, and downhill metrics; a small map preview and paired Save/Navigate actions. | A useful comparison for making route effort and the next action easy to review. |
| [komoot route map](https://mobbin.com/screens/f95ad840-1c57-421f-8bdb-47a76692c209) | A large map, compact map controls, separate Navigate and Save offline buttons, and a Surfaces breakdown. | Make offline preparation a separate decision with explicit availability. Offline basemap downloads remain a separate capability decision in the completion contract. |

## First implementation slice

1. Snapshot the current Figma Explore, search/filter, detail, Save to list and Saved states, and close the missing loading/pending/error contracts.
2. Prove the joint Store6/repaired Atom dependency tuple while completing that state/control map.
3. Implement the complete journey in Compose with persisted Store6 trail/query reads and account-owned collection membership.
4. Apply the fake backend's Offline setting, save, restart without clearing data, reopen the cached saved trail, reconnect and verify one settled logical effect. Preserve demo-server authority across the client restart.
5. Compare the installed Android journey with the Figma snapshot; then complete maps/Moments, recording/activity, profile and the remaining selected-host/state work.

This is an incremental milestone, not full completion. The original Mobbin collection's uninspected positions remain limitations of these historical observations; they no longer block implementation. Missing Trails states should be designed coherently before their affected slice. Platform-dependent work still requires real capability proof.
