# M1 interaction and presentation contract

Captured and specified September 15, 2026; world-trail catalog references revised September 18, 2026. This document closes M1's design decisions for the [completion contract](trails-completion-contract.md). It specifies behavior to implement; it does not claim that a dependency gate, APK, persistence test, or installed acceptance check passed.

The [reference inventory](trails-reference-inventory.md) identifies the original Figma frames, additive reliability states, local exports, and source hashes. The [deviation ledger](trails-reference-deviations.md) records the narrower M1 preview. Current user direction is the custom Trails design and original compass.

## Journey and ownership

Explore → normalized search and filters → selected trail detail → Save to list → Saved → collection detail → selected trail detail. Explore and Saved are the two functional preview roots. Each root retains its own navigation and scroll state. Returning from a detail restores the originating root, query, filters, and item position. A save does not force a tab switch: confirmation offers **View collection** for one selected collection and **View saved** for several. Removal from all collections also returns through **View saved**. Saved remains reachable through navigation.

| Owner | State it owns | State it does not own |
| --- | --- | --- |
| Circuit screen/Presenter | Current destination, query text, applied filters, result generation, scroll key/offset, selected Saved segment and collection ID | Authoritative trail facts, memberships, mutation settlement |
| Filter feature | Draft selectors and draft result-count request | Applied query until Apply |
| Save-flow Atom | Closed, LoadingChoices, ChoicesUnavailable, Editing, Admitting, AdmissionFailed, AdmissionUnknown; selected draft and frozen attempt reference | A persistent queue, client-selected Store6 mutation ID, retry timers, infinite repository collection, remote settlement |
| Store6-backed Trails repositories | Persisted canonical trails, ordered query memberships, account collections/membership projection, durable desired-state mutations | Screen navigation and keyboard state |
| Account service | Session restore, account partition, reconnect/startup drain, retries, remote acknowledgement/adoption, retirement | Transient sheet selections |
| Android host | Insets, IME, Back, app process lifecycle and accessible platform controls | Fake-service online/offline authority |

Opening Save enters `LoadingChoices` until both the current account's collections and the selected trail's membership are known from the repository. Reuse D11's progress treatment inside the sheet. Public Explore and cached trail detail remain usable independently. If the account projection cannot open, enter `ChoicesUnavailable` with D14's unavailable treatment, **Retry**, and **Cancel**. Save and collection editing remain disabled until the projection is known; unknown membership is never an empty set. A successful local read can establish readiness while offline.

`LoadingChoices → Editing → Admitting → Closed` means durable local admission completed. `Admitting → AdmissionFailed` means the adapter established that no durable admission occurred; it keeps the draft and offers Retry/Cancel. `Admitting → AdmissionUnknown` means the outcome is uncertain; display **Checking your save** and keep the submitted choices frozen until reconciliation resolves the outcome. A failed attempt with a known non-admission outcome may be edited. Remote pending/failed/synced status comes independently from the repository. Atom commands end after bounded admission/reconciliation results; account services own longer recovery.

### Admission identity and required adapter proof

The domain command is a versioned desired-state request for an account, trail and immutable collection set. Freeze that payload and an application command-correlation identity when the user submits. The same identity must never describe different choices. An uncertain attempt can only be reconciled against the same frozen identity/payload; do not blindly enqueue it again. Resolve the earlier outcome before admitting edited choices. Edited choices begin a new command identity after that outcome is known.

This is an application adapter requirement, not an existing Store6 lookup API. The inspected Store6 `mutate(key, ref, args)` assigns and returns its opaque mutation ID at enqueue; it does not accept a caller-selected operation ID. `pendingWrites()` does not expose argument payloads and cannot by itself match an uncertain application command to its draft. C3/data implementation must prove application command correlation and reconciliation, including cancellation/exception boundaries, before enabling uncertain-admission retry. Do not invent lookup-by-client-operation-ID support. Until that proof exists, retain the uncertainty and prevent a second admission or changed payload; the UI must not claim failure or success from an unproven assumption.

After `mutate` returns an assigned ID, preserve that durable-admission result independently of optional status refresh. A later inspection/refresh failure is not a failed save and must not trigger duplicate admission. Library mutation IDs, application command identities, and transport idempotency keys are distinct identities with explicit mappings proved by the adapter.

## Query contract

- The text selector trims leading/trailing whitespace, collapses internal whitespace, and compares case-insensitively against trail name and region. Keep the user's readable text separately from the normalized query identity.
- Difficulty selections match any selected difficulty. Empty means any difficulty. Distance endpoints are inclusive canonical meters; the upper **30+ km** position means no maximum. Feature selections match every selected feature. Query text, region, difficulty, distance and features combine with AND.
- **Sort**: Most popular is the catalog's documented recommended order; Highest rated, Shortest and Longest reorder the same membership. Region is selected only through search text in R2: the Nearby chip is retired and a checkpointed region is cleared on restore. Feature toggles live in Filters. Elevation gain is inclusive canonical metres whose upper stop means no maximum; Dog-friendly maps to the `DOG_FRIENDLY` feature; Activity matches any selected activity (Hiking on every route, Backpacking on the eight treks). All selectors combine with AND.
- Search input updates the selected query after a short debounce; IME Search submits immediately and hides the keyboard. Clear search removes text only. Clear filters removes selectors only and retains text. New query results reset scroll to the first result; returning from detail does not.
- Every request carries the normalized query key and generation. A late earlier query cannot change the current results, count, loading state, or error. A refresh of the same query keeps existing content visible.
- Counts derive from the complete current fixture selection. A count of zero is permitted only after a successful query/count. The initial M1 finite fixture does not need pagination, cursor UI, or a fake Load more control.
- The filter sheet opens a copy of applied selectors. Changing a chip, checkbox, or range updates only the draft. **Show N trails** applies the exact draft whose count is displayed. While counting, use **Counting trails…** and disable only the count-labelled action; Cancel remains available. On count failure, show **Apply filters**, **Retry count**, and **Cancel**. Applying unknown-count filters is allowed and the result view owns its loading/error state.

## Enabled-control inventory

Acceptance references are steps of the completion contract's M1 script. All runtime evidence below is **required, not yet supplied by this design snapshot**. Original source reactions are prototype hints; this table defines runtime semantics.

| Control / source | Event and transient owner | Data operation / result | Return and failure behavior | Required evidence |
| --- | --- | --- | --- | --- |
| Explore root / F01 | `SelectRoot(Explore)`; navigation | Observe current query | Restore Explore stack and scroll; repeated selection never duplicates a root | 1, 9 |
| Saved root / F06 | `SelectRoot(Saved)`; navigation | Observe account saved projection | Restore Collections/All trails and collection context | 3–5, 9 |
| Brand signature / F01 | Decorative; no click semantics | None | Screen reader receives screen heading separately | 9 |
| Search field / F01, F16 | `QueryChanged(text)`; Explore Presenter | Observe normalized query | Retain text during errors; old results cannot replace new query | 1, 6, 9 |
| IME Search / D03 | `SubmitSearch`; Explore Presenter | Immediate latest query | Hide keyboard; keep selected filters | 1, 9 |
| Search clear / D03 | `ClearQuery`; Explore Presenter | Text becomes empty | Keep applied filters; focus remains in field | 9 |
| Search Back / F16 | `Back`; host/navigation | None | First Back dismisses IME; next returns to originating Explore context without silently clearing text | 1, 9 |
| All chip / F01 | `OpenFilters(ALL)`; filter feature | Copy current selectors | Dismiss keyboard before presenting sheet | 1, 9 |
| Difficulty / Length / Elevation gain chips / F01 | `OpenFilters(section)`; filter feature | Copy current selectors | Sheet scrolls to that section | 1, 9 |
| Sort · Most popular / F01 | `Sort(sort)`; Explore Presenter | Same membership, ordered by `TrailSort` | Most popular is the fixture recommended order; no personalization claim | 1 |
| Strenuous chip / F09 | `ToggleDifficulty`; filter feature | Draft count only | Same as other difficulties | 1, 9 |
| Elevation gain slider / F09 | `ElevationChanged`; filter feature | Draft count only | Labelled native slider (DEV-20); upper stop means no maximum; Highest point is omitted (DEV-30) | 1, 9 |
| Dog-friendly switch / F09 | `ToggleSuitability`; filter feature | Draft count only | Row label toggles the switch; Kid-friendly is omitted (DEV-30) | 1, 9 |
| Activity chips · Hiking, Backpacking / F09 | `ToggleActivity`; filter feature | Draft count only; any selected activity matches | Multi-select; none selected means any activity; other R2 activities are omitted (DEV-30) | 1, 9 |
| Easy/Moderate/Hard / F09 | `ToggleDifficulty`; filter feature | Draft count only | Cancel discards; no applied count mutation | 1, 9 |
| Length range / F09 | `DistanceChanged`; filter feature | Draft count only | Accessible endpoints and text values; min cannot exceed max | 1, 9 |
| Lakes & water / Forest shade / Big views / F09 | `ToggleFeature`; filter feature | Draft count only | Entire labelled row toggles once | 1, 9 |
| Show N trails / F09 | `ApplyDraft`; filter feature → Explore | Apply matching draft query | Close sheet, run new query, reset result scroll; no literal “24” | 1, 6 |
| Count Retry / D15 | `RetryCount`; filter feature | Retry latest draft count | Keep draft and old applied query separate | 6 |
| Apply filters / D15 | `ApplyDraft`; filter feature | Apply without a claimed count | Result screen may load/fail; no fabricated zero | 6 |
| Filter Close/Cancel/scrim/swipe/Back / F09 | `DismissFilters`; feature/host | None | Discard draft and return to original query and scroll | 9 |
| Clear filters / F17 | `ClearFilters`; Explore Presenter | Retain text, clear selectors | Empty result transitions to query loading/content/error | 1, 6 |
| Trail card/image/title / F01, F16, F06 | `OpenTrail(id)`; navigation | Observe that canonical ID | Return to same list and scroll; every row resolves its own ID | 1, 3 |
| Card bookmark / F01, F16, F06 | `OpenSave(id)`; save feature | Load account collections and projected membership | Distinct click target; loading/error gate precedes editing; never also open detail | 3, 9 |
| Detail Back / F03 | `Back`; navigation | None | Restore caller and current query/collection | 1, 9 |
| Detail bookmark / F03 | `OpenSave(id)`; save feature | Current projected membership | Filled when in ≥1 collection; pending/error announced separately | 3–6 |
| Facts/rating/review excerpt / F03 | Read-only | Selected trail facts | No enabled review-submit or unrelated route | 1 |
| Save readiness Retry / D11–D14 treatment | `RetryChoices`; save feature | Reopen/read current account collections and trail membership | Save remains disabled until both are known; Cancel returns to public trail context | 6, 8, 9 |
| Collection checkbox/row / F10 | `SetCollectionDraft(id, checked)`; Atom | None until Save | Editable only in known Editing/AdmissionFailed; frozen during Admitting/AdmissionUnknown | 3, 6 |
| Save trail / F10 | `SubmitDraft`; Atom | Versioned desired collection set through application admission adapter | Freeze identity/payload, enter Admitting, block repeat taps; no caller-assigned Store6 ID assumed | 3–6 |
| Empty desired set / D16 | `SubmitEmptyDraft`; Atom | Desired empty set only when prior known membership was nonempty | Explicit **Remove from saved** action; **Keep editing** returns to draft. Never-saved zero-selection Save is disabled/no-op | 6 |
| Save Close/Cancel/scrim/swipe/Back / F10 | `DismissSave`; Atom/host | None while LoadingChoices/ChoicesUnavailable/Editing/known AdmissionFailed | Discard unsubmitted draft; bounded Admitting/AdmissionUnknown uses outcome guard and never treats dismissal as rollback | 3, 6, 9 |
| Try saving again / D07 | `RetryAdmission`; Atom | Retry only a known non-admission outcome using the defined adapter protocol | Changed choices get a new identity. Unknown outcomes require same-payload reconciliation; no blind re-enqueue | 6 |
| View collection / F14, D08 | `OpenCollection(id)`; navigation | Observe sole selected collection | Close transient confirmation; Saved root/collection opens once | 3–5 |
| View saved / multiple or zero collections | `SelectRoot(Saved)`; navigation | Observe shared saved projection | Deterministic destination; never choose an arbitrary collection | 3–5 |
| Collections / All trails / F06 | `SelectSavedSegment`; Saved Presenter | Collection rows or deduplicated union | Preserve per-segment scroll; counts are distinct trail membership | 3–5 |
| Collection card / F06 | `OpenCollection(id)`; navigation | Account collection ID and membership | Empty collection is a valid state; unknown ID produces unavailable/Back | 3–5 |
| Collection Back / D09 | `BackToSaved`; navigation | None | Restore Saved segment and scroll; no root duplication | 9 |
| Collection row bookmark / D09 | `OpenSave(trailId)`; save feature | Shared projected set | Removing membership updates row/count everywhere; removal from this collection does not remove other memberships | 3–6 |
| Explore trails / F20, D14 | `SelectRoot(Explore)`; navigation | Existing Explore query | Known empty and unavailable copy remain distinct | 8, 9 |
| Retry / F18, D02, D05, D14 | `RetryRead(key)`; Presenter | Same-key refresh or account reopen | One in-flight retry; retain query and any content | 6, 8 |
| Open saved trails / F18, D05 | `SelectRoot(Saved)`; navigation | Read local account projection | Saved may itself show unavailable; do not promise data without observing it | 4, 8 |
| Try sync again / D10 | `RetryPending`; account service | Bounded retry of eligible pending work, same identity | Disable while drain active; parked schema/terminal work has no misleading Retry | 5, 6 |
| Demo account / D12 | `EnterDemoAccount`; account service | Restore/establish documented fixture account | Old account isolation remains; failure shows Retry and no false active session | 7, 8 |
| Developer Offline / acceptance drawer | `SetNetworkMode`; debug account/platform service | Persist configuration and apply NetworkGate | Show applying/applied distinction; restore before first fetch/drain | 2, 4, 5, 8 |
| Developer account switch/sign-out | `RetireAccount`; account service | Retire old graph; switch durable session | Clear old UI; preserve old-account work only in old partition | 7 |

The developer drawer is sample tooling and remains visually separate from trail browsing. Airplane mode is not an input to its in-process `NetworkGate`.

## Reliability-state matrix

`Fxx` identifies an original frame; `Dxx` identifies an actual added frame. Expanded state faces on the additive board show copy, control states and hierarchy; F09/F10 remain the sheet-container authority. Layout may expand for large text and IME insets.

| Situation | View and truthful content | Recovery / lifetime |
| --- | --- | --- |
| Initial query, no local result | D01: skeleton and labelled progress; query remains editable | Content, successful empty F17, or unavailable D05/F18 |
| Refresh while content exists | Keep content, small progress near count | Failure D02 says **Couldn’t refresh · Showing saved trails**, keeps content and offers Retry |
| Query succeeds with no rows | F17 **No trails match yet** | Clear filters retains query; editing is available |
| Offline query has no cached membership | D05 **Offline · Saved trails are still here** | Retry/Open saved; no invented empty result/count |
| Offline cached detail exists | F03 with cached facts and availability/pending notice | Navigation remains usable; refresh failure never hides facts |
| Detail ID absent from local cache and fetch fails | Unavailable panel using D05 treatment, Back and Retry | Keep ID/caller; never show another trail's facts |
| Image unavailable independently of facts | Fixed-ratio Soft placeholder with trail name | Facts/save controls remain; no broken-image icon or implied downloaded map |
| Focused search / cleared query | D03; native keyboard, clear action, selected filters | Keyboard Back first; clear query preserves filters |
| Draft count loading / failed | D04 / D15 | Loading has no numeric count; failure can apply selectors without count |
| Save opened before account collections/membership are known | D11 progress treatment within F10; D14 treatment on failure, Retry/Cancel | Save/edit disabled; public Explore/detail remain usable; unknown membership never becomes an empty desired set |
| Save choices editing | F10 after both projections are known | Close cancels draft; zero selected offers D16 only if prior membership was nonempty; otherwise Save disabled/no-op |
| Durable local admission running | D06 | One finite admission; no success until commit; no remote wait |
| Known local non-admission after journal/database failure | D07 **Couldn’t save · Your choices are still here** | Draft retained; Retry or Cancel; changed choices get a new identity; bookmarks/counts stay pre-admission |
| Local admission outcome uncertain | D11 progress treatment with **Checking your save…** and the frozen submitted choices | Application correlation/reconciliation proof required; no payload edits or duplicate admission until resolved |
| Admission committed, remote pending | **Saved on this device · Waiting to sync** generally; D08 **when you’re back online** only with applied Offline | Shared repository projection updates all screens; survives restart; active drain may say **Syncing…** |
| Remote retryable failure / exhausted automatic retry budget | D10 **Sync needs attention · Local save is kept** | Local desired state remains; manual eligible retry retains the immutable transport generation and idempotency key |
| Unknown payload/schema or decoder-incompatible parked work | D13 **App update needed to sync · Saved here** | Keep intent and explain app update/recovery requirement only for this actual incompatibility; no endless Retry or false Synced |
| Other permanent/parked failure | D13 notice structure with the actual cause and supported recovery action | Do not promise that an app update resolves unrelated authorization, validation, projection or policy failures |
| Server acceptance uncertain; no durable acknowledgement recorded | Pending indication until receipt recovery | Account service may resend the same immutable transport generation idempotently; one logical effect |
| Durable ACKED record exists; local adoption interrupted | **Finishing your save** / pending until local adoption completes | Resume adoption from durable acknowledgement; never push that acknowledged generation again |
| Rapid save/unsave/save | Latest admitted desired set renders; pending remains if needed | Per-trail ordering preserves final intention; delayed earlier acknowledgement cannot roll UI back |
| Saved known empty | F20 | Explore CTA; known collection with zero trails uses same treatment and its title |
| Saved uninitialized/unavailable | D11 while restoring; D14 on failure | Never turn unavailable into F20 or display fabricated counts |
| Collection detail | D09; own title, actual membership count, trail rows, pending summary | Each row opens its own trail; mutations update live projection |
| Force-stop/relaunch | D11 only while identity/config/db open; then restored cached screen/D08 | Keep account, applied Offline, navigation context, membership, queue and backend receipts |
| Sign-out/account switch during pending work | D12; old private projection immediately unavailable | Retire streams/drain; old work is retained in the old partition and resumes only on same-account return |
| Larger text / TalkBack | F19 plus native measured layout | Reflow title/facts, scroll sheet; no fixed-height text clipping; selected/pending semantics announced |

Unsubmitted save drafts are transient and may be lost after process death. Submitted membership must survive. The restored navigation record contains IDs and view state, not a second authoritative copy of repository data. If a saved destination no longer resolves, show its unavailable state with a clear return path.

## Account and remote-authority recovery

Restore the durable demo session identity and debug backend configuration first. Then open that account's Store6 stores, attach projections, and start eligible draining. Apply the fake backend's Offline mode before reads or drains. A UI setting may show **Applying…** until the backend confirms the applied mode; acceptance waits for that confirmation.

Retirement cancels the old account's readers and drain owner and invalidates its generation. No late response can enter a new account. Old-account queued work stays partitioned, does not drain while signed out, and resumes only when that same account is restored. Do not automatically delete pending work on sign-out. Full demo reset is a separate explicit destructive debug action and is excluded between offline-save and reconnect acceptance steps.

The fake server must persist canonical server rows and applied-operation receipts independently of client query/value/journal stores. Restoring only the client queue while resetting the server is not recovery. Each receipt binds the account and transport idempotency key to the immutable payload/schema version, so duplicate delivery cannot silently apply different commands under one key. The application adapter proves how that transport identity maps to the Store6 mutation and application command identities.

## Compose and TrailsTheme translation

Use reusable native components under `foundation/designsystem`; `feat` owns filters/save sheets, and `screen` owns Explore/detail/Saved/collection routes. Existing `TrailsTheme` accessors stay the integration API. Do not add HeroUI JavaScript or route these actions to old ski screens.

| Figma basis | Compose counterpart | Presentation and semantic states |
| --- | --- | --- |
| Button `147:633` | `TrailsButton` with tones Hero (Citron/Ink), Commit (Dark/White), Secondary (Soft/Ink); `TrailsIconCircle` for icon actions | Pill shape; 52 dp reference height; min 48 dp Android target; loading, disabled, pressed and accessible label |
| Chip `153:1092` | Selectable Trails filter chip | Forest/white selected, Soft/Ink unselected; selected semantics; 44 px reference expands touch target to 48 dp |
| SearchField `153:959` | Trails search field with native text input | Persistent accessible label, IME Search, clear affordance, focus ring, error/loading result states |
| Trail card `150:21` | `TrailSaveCard` | Photo 233 dp with heart circle (download circle deferred, DEV-24); optional highlight chip; facts row: star, rating (count), difficulty marker + name, length |
| Sheet `157:721` | ModalBottomSheet through Trails styling | White, 28 dp top radius, 24 dp side padding, scrollable body, IME/navigation insets, native dismiss handling |
| Checkbox `158:1542` / control `158:1451` | Labelled checkbox row | Forest selected control, check mark plus checked semantics; whole row ≥48 dp |
| Slider `158:1338` | Two native endpoint sliders with visible minimum/maximum labels (DEV-20) | Canonical range; separate accessible adjustment actions and kilometer/no-maximum states on the actionable nodes |
| Bottom navigation `161:1108` | `TrailsFloatingNav` with selected semantics | Final five roots (Explore, For You, Navigate, Saved, Activity) in the R2 floating pill; M1 preview two roots; the compass stays the brand mark |
| Spinner from HeroUI key `66e1079db7ec600360775393926b3ed01fe1e0c1` | Labelled progress indicator | Reduce motion without hiding loading meaning; no arbitrary timed-success transitions |
| Status line `275:4329`, Toast `275:4292`, Decision sheet `275:4339` | `TrailsStatusLine`, `TrailsToast`, decision layout inside the save sheet | One sentence beside its subject; completion toast above navigation, polite live region, dismisses after 5 s; pending and read staleness remain independent |

| Theme role | R2 value | Translation |
| --- | --- | --- |
| Background and Surface | `#FFFFFF` | `TrailsTheme.colors.background` / `.surface` |
| Foreground / Ink | `#171E14` | Primary text, outlined chip borders inherit Border |
| Muted | `#545A52` | Secondary text; 6.6:1 on white |
| Border | `#E6E8E4` | Dividers, tag outlines, grabber |
| Accent / Forest | `#1D4B35` | Selected chips, icons, brand surfaces; never a button fill |
| Citron | `#A9F184` | Hero action fill with Ink text (11.7:1) |
| Dark | `#0D1F18` | Commit action fill and toast surface with white text |
| Soft | `#F4F5F4` | Secondary buttons, unselected chips, search field |
| Clay | `#BC624A` | Error accent/stroke only |
| Warning / Danger | `#8A4B12` / `#A5352B` | Status indicators (Attention / Failed) |

Local variables include spacing 4/8/12/16/20/24/32/48 and radii 16/24/32/999. Use the existing spacing API and add missing semantic values deliberately. Sheet radius 28 and common screen side inset 20 are measured design values. Reference title is Manrope Bold 34/40; detail heading 30/36; subheading Manrope Semibold 20/26; reading Inter Regular 17/25; secondary Inter Regular 15/21; controls Inter Semibold 17/22; tabs Inter Medium 12/16. Use `sp` and actual system font scale.

Bundle the local Manrope/Inter assets and accompanying OFL licenses; no runtime font download is needed. The source font axis definitions and selected weights must be respected by Compose. Runtime typography still needs Android measurement and large-text proof; availability of TTF files alone does not prove font integration.

Use real status/navigation bars and window insets on Android. Do not draw the Figma **9:41**, home indicator, device mask, or keyboard into app content. The compass master is unmasked and square; its Android adaptive/monochrome translation is a platform asset task and must preserve source alignment. The Figma source is not proof of launcher-mask safety.

## Accessibility and validation boundary

- Android controls have at least 48 dp touch bounds even where the reference visible shape is 44 px. Icons need meaningful labels: **Save Half Dome**, **Edit saved collections for Half Dome**, **Back**, **Filters**.
- Announce applied filters, checkbox state and selected navigation without relying on color. Announce local save admission once; pending/synced updates must not repeatedly steal focus.
- Trap traversal within an open sheet; restore focus to its invoking bookmark/filter control on dismissal. Mark screen titles as headings. Decorative brand, gradients and repeated photo labels do not add redundant focus stops.
- Reflow trail facts to additional rows with larger text; allow multi-line collection names and buttons; keep primary actions reachable above IME/navigation insets. Static Figma D states and F19 do not pass TalkBack or font-scaling checks.
- The design worker performed Figma geometry, export, asset/hash, and selected visual checks only. The serialized Gradle runner and installed M1 acceptance script remain the authority for build/runtime behavior.

## Remaining roots preview (September 20, 2026)

The five-root pill is complete. For You, Navigate and Activity are previews: seeded data, no map provider, no recorder ([design](superpowers/specs/2026-09-20-trails-remaining-roots-design.md)).

| Control / Frame | Intent; owner | Data effect | Behaviour | Cells |
| --- | --- | --- | --- | --- |
| Feature card / F02 | `OpenTrail(featured)`; For You Presenter | None | Opens the featured trail's detail in the For You stack | 10 |
| Recommendation row / F02 | `OpenTrail(trail)`; For You Presenter | None | Rows exist only for trails cached in the catalog | 10 |
| Heart circle / F02, F07 | `SaveTrail`; save feature | Draft until Save | Same sheet, toast and status lines as Explore | 10, 12 |
| Trail name pill / F25 | `OpenTrail`; Navigate Presenter | None | The current trail: last opened, else first saved, else Half Dome | 11 |
| Start recording / F25 | `StartRecording`; Navigate Presenter | None | Dark toast `Recording is coming soon`, no tick, dismisses after 5 s (DEV-31) | 11 |
| Activity row / F07 | `OpenTrail(trailId)`; Activity Presenter | None | Opens the trail's detail in the Activity stack (DEV-35) | 12 |
| Explore trails / F07 empty | `Explore`; Activity Presenter | None | Selects the Explore root | 12 |
| Try again / F02, F07, F25 | `Retry`; each Presenter | Refreshes the feed or trail | Cached content stays visible; status line beside its subject | 10–12 |

Query and data contract: activity totals cover the device's current calendar month; the seven bars are the last seven local days ending today; dates read `Today`, `Yesterday`, else `Sep 10`. Fixtures are seeded once per account on first request and read verbatim afterwards; they never change on refresh. Welcome discloses that activity history and recommendations are sample data.
