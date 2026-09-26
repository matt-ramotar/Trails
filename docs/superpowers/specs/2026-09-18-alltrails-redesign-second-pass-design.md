# Trails Figma · AllTrails redesign, second pass

Date: 2026-09-18. Scope: the Figma file `4B7GK9ndPVQ1BFIKGg0Zqj`, page `142:2` (Trails v4 · AllTrails redesign). No app code changes.

## Problem

The page already carries a first adaptation of the 13 AllTrails captures in `redesign/` (IMG_0470–0476, 0478–0483). It reproduces the structure (search + filter chips, photo trail cards, floating Map pill, floating five-tab navigation, Saved tabs, Filters sheet, Save/Download/Map detail actions) but leaves out the elements that make the September 2026 AllTrails release recognisable. This pass closes those gaps while keeping the Trails identity (compass, Manrope/Inter, Field Notes tokens, metric units, generated photography).

## Gaps closed

| Area | AllTrails capture | Change in Figma |
| --- | --- | --- |
| Difficulty markers | Coloured shape before the difficulty word (Easy circle, Moderate square, Hard triangle, Strenuous diamond) on cards, rows, detail and filters | New local component set `Trails / Difficulty marker` (4 variants). Exposed on the Trail card as an INSTANCE_SWAP property; used in For You rows, the detail rating row and the Filters chips |
| Trail card | Download and heart circles on the photo; star icon; optional highlight chip ("Great views") top-left | Card gains a download button, a star icon, structured Rating / Difficulty / Length text properties (replacing the single Facts string) and a boolean-controlled highlight chip. All existing instances are migrated |
| Explore | "All" chip carries the filter icon | Prefix icon on the All chip (Explore, Filters backdrop, Map shelf) |
| Trail detail | Share / heart / more circles; underlined rating row with marker; facts with dividers (Length, Elev. gain, Est. time, Loop); "Show more"; On-trail directions and expand controls on the route map; Preview and Photo tour media cards; Customize route / Get directions; Top sights list; tag chips | All added inside the existing scroll content. A second frame (26 / Trail detail · Route and sights) shows the scrolled state |
| Filters | Strenuous; Length, Elevation gain, Highest point, Distance away; Dog-friendly / Kid-friendly switches; Activity chips; sticky Show button | Sheet body becomes a clipped scroll region with the full option set. A second frame (27 / Filters · More options) shows the scrolled state |
| For You | "Based on your activity" + "More like …"; rows with star, marker, heart circle, separators | Copy and rows updated |
| Saved | Favorites placeholder tile; Recommended lists section | Collection tile becomes a variant set (Cover=Photo / Cover=Placeholder with icon swap); Favorites tile and Recommended lists added |
| Map | Layers / 3D / compass stack, location button, download and draw buttons, scale bar | Controls added around the existing map |
| Sight detail | Map with POI pins and a sheet: name, type, distance from trailhead, elevation gain, description, photo pager | New frame (28 / Sight detail) |
| Paywall | Trails+ trial screen | Not adapted. Trails has no subscription; recorded in the notes frame |

## Approach

1. Foundations first (components frame): difficulty markers, Trail card upgrade with instance migration, Collection tile variants.
2. Screen edits in place, one section per `use_figma` call, hiding rather than deleting superseded nodes.
3. New frames are appended to the section grid (row 6) with numbered labels; the section and its footer note grow to fit.
4. Colours bind to the existing Field Notes variables; type uses the existing Manrope/Inter pairing; library components come from the linked HeroUI Pro kit (Button, Chip, Switch, icons).
5. Every edited frame is screenshot-verified; the notes frame records the second pass.

## Out of scope

App code, the M1 reliability board, the Brand identity and Messaging pages, subscription/paywall UI, activity pictogram icons the kit does not contain (Activity chips are text-only).

## Status (2026-09-18)

Implemented in the Figma file. New local component sets: `Trails / Difficulty marker` (`257:2858`, variants Easy `257:2850`, Moderate `257:2852`, Hard `257:2854`, Strenuous `257:2856`) and `Trails / Collection tile` (`257:2867`, Cover=Photo `236:2112`, Cover=Placeholder `257:2859`). Trail card properties: `Rating#259:0`, `Difficulty#259:1`, `Length#259:2`, `Difficulty marker#259:3`, `Show highlight#258:0`. New frames: 26 `264:3418`, 27 `264:3966`, 28 `263:3756`. Known limitation: the HeroUI Switch has no off state, so the Dog-friendly / Kid-friendly toggles render on.

## Follow-up (2026-09-18, later): button hierarchy and messaging

**Why buttons disagreed.** Three generations of "primary" coexisted: accent forest from the v3 Field Notes screens and the M1 board (15), citron from the AllTrails pass (7), and near-black pills copied from AllTrails' sheet commits (4). Rule applied to 31 instances: citron for the single hero action a screen exists for (Map, Save, Start recording, Pause/Resume, Save activity); `dark` for commits and empty-state primaries (Show 24 trails, Explore trails, Finish hike, Try again, Search, Apply filters); soft for secondary actions; white circles for icon actions. Accent forest is never a button fill. Three white "Try again" primaries on the M1 board became soft secondaries.

**Messaging.** The card-style Toast and Inline notice (icon in a circle, title plus description, button inside the card) were archived on the Components · Messaging page (hidden frame `Archive / Messaging before second pass · Sep 18`) and replaced by:

- `Trails / Toast` `275:4292` — dark line above the navigation; props `Message#275:1`, `Action#275:2`, `Show action#275:3`, `Show check#275:0`.
- `Trails / Status indicator` `275:4328` (Pending `275:4299`, Offline `275:4305`, Failed `275:4307`, Attention `275:4314`, Info `275:4321`) and `Trails / Status line` `275:4329` — props `Indicator#275:4`, `Message#275:5`, `Action#275:6`, `Show action#275:7`; override the message fill to foreground for Failed and Attention.
- `Trails / Decision sheet` `275:4339` — props `Title#275:8`, `Body#275:9`; buttons are nested Button instances.

Swapped in on screens 14 and 15 (toast 16 px above the nav, action shortened to "View"), Collection detail, and all 15 M1 board states, whose copy became one short line each (for example "Saved on this device · Waiting to sync", "Couldn't count trails · Filters are kept"). The reference frame is now `Trails / Messaging · Second pass · Sep 18` `272:3850` at the old frame's position; the old frame is hidden at x 6100. The M1 evidence exports under `docs/evidence/m1` still show the September 15 card messages and accent buttons.
