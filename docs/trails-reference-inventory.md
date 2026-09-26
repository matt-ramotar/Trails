# Trails design reference inventory

Snapshot: September 15, 2026. Authority: [Trails v3 / Field Notes](https://www.figma.com/design/4B7GK9ndPVQ1BFIKGg0Zqj/Trails?node-id=153-97), file `4B7GK9ndPVQ1BFIKGg0Zqj`, page `142:2`. The current user's original-compass instruction supersedes the older mountain/path icon direction.

**Live refresh, September 16:** [Continuation design evidence](evidence/m1/continuation-design/README.md) verifies the same 20 original frames, 16 numbered reliability states, 34 variables, 17 text styles, component identities, source geometry and original compass. All 45 frozen evidence hashes and five bundled font/license/atlas copies still match. The refresh preserves the original asset snapshot and records the selected-trail review-excerpt omission found during source review for implementation correction.

## Reproducible handoff

All build-relevant source material for this design handoff is local under [evidence/m1/design](evidence/m1/design). No expiring asset URL or Desktop directory is a build input.

- [Source manifest](evidence/m1/design/source-manifest.json): Figma IDs, screenshot paths, original asset provenance, font revision and licenses.
- [SHA-256 manifest](evidence/m1/design/SHA256SUMS): hashes of the captured evidence/assets. From the repository root: `shasum -a 256 -c docs/evidence/m1/design/SHA256SUMS`.
- [Original frame snapshot](evidence/m1/design/figma-frames-2026-09-15.json): all 20 frames, text, dimensions, and image hash/crop transforms.
- [Tokens and typography](evidence/m1/design/figma-tokens-2026-09-15.json): exact local Trails variable values/aliases and text styles. Legacy file variables are excluded by their IDs; this is the current Trails token set.
- [Components](evidence/m1/design/figma-components-2026-09-15.json): source component keys, variant counts and representative variant IDs. Button has 168 library variants and Chip 60; the snapshot records relevant representative variants, not a claim that every library variant is used.
- [Added-state snapshot and geometry](evidence/m1/design/figma-added-states-2026-09-15.json): 16 new 390 × 844 states and structural review.
- [Capture notes](evidence/m1/design/capture-notes.md): export procedure, corrected first failures, visual review and evidence limits.
- [M1 interaction contract](trails-m1-design-contract.md): control/event/owner/data/recovery specification and acceptance-script references.

The [new Figma board](https://www.figma.com/design/4B7GK9ndPVQ1BFIKGg0Zqj/Trails?node-id=217-1530), **M1 / Reliability and recovery · 2026-09-15**, is additive and reversible. The original app section still contains exactly 20 screen frames. The board contains 16 implementation state faces and a full-resolution landscape source copy. It is a static review surface; runtime behavior is defined by the contract and must be implemented and tested.

## Original 20-frame census

Every original frame is 390 × 844. Local screenshots were captured for all 11 M1 source frames and the production compass. Later milestone rows retain their live source IDs and full text/image census; they do not yet have implementation evidence.

| Ref | Figma node / frame | Milestone | Local M1 capture / remaining design coverage |
| --- | --- | --- | --- |
| F01 | `148:52` Explore | M1, M2, M4 | [Explore](evidence/m1/design/screenshots/explore.png); M1 controls specified; map/Moments/profile remain later |
| F02 | `148:55` Moments | M2 | Feed switching, actions, empty Following and per-moment trail identity remain |
| F03 | `148:58` Trail detail | M1, M2, M3 | [Detail](evidence/m1/design/screenshots/trail-detail.png); View route/Start remain M2/M3 |
| F04 | `148:61` Map | M2 | Provider/attribution, route rendering, camera/selection and failure/location states remain |
| F05 | `148:64` Recording | M3 | Permission, sampling/timer, lifecycle and durable recording states remain |
| F06 | `148:68` Saved | M1, M4 | [Saved](evidence/m1/design/screenshots/saved.png); membership/browsing specified; management M4 |
| F07 | `148:71` Activity | M3 | Persisted history/totals and activity detail/empty/failure remain |
| F08 | `148:74` Profile | M4 | Shared identity/preferences/routes and persistence failure remain |
| F09 | `148:77` Filters | M1 | [Filters](evidence/m1/design/screenshots/filters.png); draft/count/apply/cancel specified |
| F10 | `148:80` Save to list | M1 | [Save sheet](evidence/m1/design/screenshots/save-to-list.png); finite draft/admission and empty-set semantics specified |
| F11 | `159:856` Recording paused | M3 | Pause/Resume sampling/timer/recovery proof remains |
| F12 | `159:858` Finish hike | M3 | Keep recording/Close/Finish and duplicate finish semantics remain |
| F13 | `159:860` Hike complete | M3 | Durable save, discard confirmation and unsaved-session recovery remain |
| F14 | `159:862` Saved confirmation | M1 | [Confirmation](evidence/m1/design/screenshots/saved-confirmation.png); replaced runtime copy separates local admission from sync |
| F15 | `160:1096` Activity saved | M3 | Idempotent activity submission and totals/history convergence remain |
| F16 | `197:1158` Search results | M1 | [Search](evidence/m1/design/screenshots/search-results.png); query/key/count/back behavior specified |
| F17 | `197:1172` No results | M1 | [No results](evidence/m1/design/screenshots/no-results.png); successful empty only; clear selectors preserves text |
| F18 | `197:1186` Offline | M1 | [Original offline](evidence/m1/design/screenshots/offline.png); unconditional saved-availability claim superseded by D05/D14 |
| F19 | `197:1200` Larger text | M1–M4 | [Larger text](evidence/m1/design/screenshots/larger-text.png); native scaling/TalkBack still unverified |
| F20 | `197:1260` Saved empty | M1 | [Saved empty](evidence/m1/design/screenshots/saved-empty.png); distinguish known empty from uninitialized/unavailable |

## Actual added M1 states

All entries below exist in the live additive board and have local exports. They were created during this pass; they are not hypothetical variants or preexisting user-approved screens. F09/F10 remain the ordinary sheet-container reference; added expanded state faces specify content and behavior for failure/large-text cases.

| Ref | Live node | Added state | Local export |
| --- | --- | --- | --- |
| D01 | `218:1648` | First-load query and skeleton | [D01](evidence/m1/design/screenshots/d01.png) |
| D02 | `218:1694` | Refresh failure with retained content | [D02](evidence/m1/design/screenshots/d02.png) |
| D03 | `218:1772` | Focused search / clear / submit | [D03](evidence/m1/design/screenshots/d03.png) |
| D04 | `218:1908` | Filter draft with count loading | [D04](evidence/m1/design/screenshots/d04.png) |
| D05 | `219:1685` | Offline query with no local result | [D05](evidence/m1/design/screenshots/d05.png) |
| D06 | `219:1772` | Finite local save admission | [D06](evidence/m1/design/screenshots/d06.png) |
| D07 | `219:1849` | Local admission failure retaining draft | [D07](evidence/m1/design/screenshots/d07.png) |
| D08 | `219:1936` | Durably saved locally, remote pending with Offline applied | [D08](evidence/m1/design/screenshots/d08.png) |
| D09 | `219:2012` | Collection detail with pending membership | [D09](evidence/m1/design/screenshots/d09.png) |
| D10 | `219:2087` | Retryable sync attention | [D10](evidence/m1/design/screenshots/d10.png) |
| D11 | `219:2152` | Account/session restoration | [D11](evidence/m1/design/screenshots/d11.png) |
| D12 | `219:2198` | Signed out, work retained under old account | [D12](evidence/m1/design/screenshots/d12.png) |
| D13 | `219:2253` | Parked schema/decoder-incompatible work, no misleading Retry | [D13](evidence/m1/design/screenshots/d13.png) |
| D14 | `219:2307` | Saved data unavailable, distinct from empty | [D14](evidence/m1/design/screenshots/d14.png) |
| D15 | `219:2370` | Filter count failure, apply without count | [D15](evidence/m1/design/screenshots/d15.png) |
| D16 | `219:2444` | Previously saved, now no selected collections / explicit remove action | [D16](evidence/m1/design/screenshots/d16.png) |

The [board overview](evidence/m1/design/screenshots/m1-states-board.png) is useful for orientation. Individual 390 × 844 exports preserve readable details. Save-sheet collection/membership readiness and uncertain-admission states reuse D11/D14 treatments inside the F10 shell, as specified in the M1 contract. They are specified compositions, not additional live frames. Application command correlation/reconciliation remains a C3/data adapter proof; no unsupported Store6 client-ID lookup is assumed.

## Assets and provenance

| Asset | Source and local file | Reuse boundary |
| --- | --- | --- |
| Original compass production master | Figma `204:1685`; [master SVG](evidence/m1/design/assets/trails-compass-master.svg) | 1024 × 1024, unmasked, Forest background. Original compass has 576 × 576 instance bounds at (224,224); visible vector extrema are (260,260)–(764,764). |
| Original compass mark | Figma `172:1138`; [48 px SVG](evidence/m1/design/assets/trails-compass-mark.svg) | HeroUI compass component provenance retained; current user's chosen brand asset. Do not substitute the rejected mountain icon. |
| Compass foreground | Figma `210:1704`; [foreground SVG](evidence/m1/design/assets/trails-compass-foreground.svg) | Transparent 1024 canvas; same alignment as master. Platform masks/materials are separate. |
| Landscape triptych | Original `148:82`, copied at source resolution to `220:1975`; [1536 × 1024 PNG](evidence/m1/design/assets/trails-generated-landscapes-full.png) | Figma annotation `166:1134` identifies an **Original generated triptych**. Image hash `c69fac26966a4da1fc34d94c4fcbb37642bc22ab`. Use as sample concept photography, not verified real-location photography. |
| Alpine/Pine/Sunset crops | Original frame JSON stores per-node image transforms | Left/middle/right thirds correspond to lake/forest/ridge. Use the retained atlas/crop coordinates; no runtime Figma request. |
| Manrope variable font | Google Fonts revision `1ac2012c34919f5fa2675aacf723fa98edb30b5f`, `ofl/manrope/Manrope[wght].ttf`; [local TTF](evidence/m1/design/assets/fonts/manrope-variable.ttf) and [OFL](evidence/m1/design/assets/fonts/manrope-OFL.txt) | Original bundled font and license; selected heading weights 600/700. [Source directory](https://github.com/google/fonts/tree/1ac2012c34919f5fa2675aacf723fa98edb30b5f/ofl/manrope). |
| Inter variable font | Same revision, `ofl/inter/Inter[opsz,wght].ttf`; [local TTF](evidence/m1/design/assets/fonts/inter-variable.ttf) and [OFL](evidence/m1/design/assets/fonts/inter-OFL.txt) | Original bundled font and license; selected body/control weights 400/500/600. [Source directory](https://github.com/google/fonts/tree/1ac2012c34919f5fa2675aacf723fa98edb30b5f/ofl/inter). |

The full HeroUI Pro kit is not vendored here. Its linked component keys and sampled variants are design provenance. The implementation translates them to native Compose components with TrailsTheme. The saved SVGs and generated concept image are the specific requested asset handoff; this document does not assert a broader redistribution right over the commercial kit.

## Remaining milestone and platform contract

M1 design implementation can proceed from this snapshot once the joint Store6/repaired-Atom gate passes. The current design handoff supplies all enabled M1 behavior and source assets. It does not pass C3 or the installed M1 acceptance script.

M2 retains Map, Moments, feed selection and corresponding loading/empty/location/provider states. M3 retains recording, permission/lifecycle/recovery, completion/save/discard, activity detail/history and derived totals. M4 retains Profile/preferences, create/rename/delete collection management, the complete navigation shell and remaining polish. The [deviation ledger](trails-reference-deviations.md) keeps these omissions visible.

Android is required for the current installed preview. iOS, Desktop/JVM and Web runtime acceptance remain undecided. Their existing compilation targets must not be silently removed; neither this Figma file nor a JVM compile selects full runtime/offline parity. A final platform decision is still needed before declaring the whole sample finished.

## September 18 revision R2

Design revision R2 (live inspection September 18, 2026) supersedes the September 15 token and component rows above for implementation while the frozen evidence set stays intact. Section `153:97` now holds 28 frames (F21–F28 listed in the completion contract). Local component sets: Difficulty marker `257:2858` (Easy `257:2850`, Moderate `257:2852`, Hard `257:2854`, Strenuous `257:2856`), Collection tile `257:2867` (Cover=Photo `236:2112`, Cover=Placeholder `257:2859`), Toast `275:4292`, Status indicator `275:4328`, Status line `275:4329`, Decision sheet `275:4339`. Trail card `150:21` properties: `Rating#259:0`, `Difficulty#259:1`, `Length#259:2`, `Difficulty marker#259:3`, `Show highlight#258:0`. Token values, the button rule and the messaging rule are recorded in the completion contract. Card messaging components are archived (hidden) on the Components · Messaging page. The Trails+ paywall was not adapted. Reference: `docs/superpowers/specs/2026-09-18-alltrails-redesign-second-pass-design.md`.
