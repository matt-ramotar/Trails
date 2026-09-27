# Native component adaptations

Trails implements HeroUI Native component geometry, states, and motion in Compose.
The reference is the public Native source at commit
[`122f63db3159f2192a9de38c0e85e5ae9004473a`][reference], whose package version is
**1.0.10**. This is a source reference, not a runtime dependency. Trails does not
install HeroUI or React Native packages and does not require Pro components.

HeroUI component styling takes precedence when an older Trails Figma component
conflicts with it. Keep the Trails palette, font families, and original compass.
Use the pinned component's semantic typography roles and metrics with those fonts.
The [theme variables][variables], [derived theme tokens][theme], and individual
component styles define the reference roles. Screen copy and domain drawings
remain Trails content.

## Shared adapters

Reusable visual components belong to
[`foundation/designsystem`](../multiplatform/foundation/designsystem), in the
`org.mobilenativefoundation.trails.foundation.designsystem.component` package.
Features and screens compose these adapters rather than restyling controls.
The table is the component contract: appearance, interaction states, and motion
must be reviewed against the linked source when changing an adapter.

| Trails component | Pinned Native reference | Adaptation contract |
| --- | --- | --- |
| `TrailsSlider` | [Slider styles][slider], [behavior][slider-behavior] | Continuous 20 dp rounded rail, accent fill, 28 × 20 dp thumb and light inset knob. Preserve bounds, steps, unit labels and native range actions. |
| `TrailsSwitchRow` | [Switch][switch], [ControlField][control-field] | One labeled row owns the toggle. The 48 × 24 dp visual track contains a 28 × 20 dp thumb. |
| `TrailsCheckbox`, `TrailsCheckboxRow` | [Checkbox][checkbox], [ControlField][control-field] | 24 dp rounded field and selected indicator. The row owns checked state, enablement and one native toggle action. |
| `TrailsFilterChip` | [TagGroup][tag-group] | Selectable soft tags with selection and disabled states; selection remains distinct from informational chips. |
| `TrailsSegmentedControl` | [Tabs styles][tabs], [motion][tabs-motion] | Primary soft track, surface indicator and native single-selection semantics. Labels can scroll at large font sizes. |
| `TrailsSelect` | [Select styles][select], [popover presentation][select-behavior], [popup motion][select-popup-motion], [indicator motion][select-indicator-motion] | Field and focusable option popup, selected-option indicator, disabled state and native keyboard/Back dismissal. Explicit Native entrance/exit motion runs after placement. |
| `TrailsSearchField` | [SearchField][search-field], [Input][input] | Filled field, focus treatment, magnifier and stable clear-action space. Native text editing and Search IME remain available. |
| `TrailsButton` | [Button styles][button], [behavior][button-behavior] | Primary, soft and ghost action hierarchy with pressed, focused, disabled and loading states using Trails action colors. |
| `TrailsIconButton`, `TrailsIconCircle` | [Button styles][button], [behavior][button-behavior] | Icon-only targets with pressed and focused states. These wrappers do not expose disabled or loading parameters. |
| `TrailsTextAction` | [Button ghost variant][button] | Local actions such as expand, retry and toast actions use a shared ghost-button treatment. |
| `TrailsLinkButton` | [LinkButton][link-button] | External navigation links, including photograph source and license URLs. Preserve the exact destinations. |
| `TrailsPressable`, `trailsPressFeedback`, `trailsFocusRing` | [PressableFeedback][pressable], [motion][pressable-motion] | Shared feedback and visible focus for actions and independently clickable card/row surfaces. Avoid adding a second Material ripple or pressed effect. |
| `TrailsSpinner`, `TrailsLoading` | [Spinner][spinner], [timing and sizes][spinner-constants] | Native spinner geometry and motion, including the indicator inside loading buttons. Keep accessible loading text and progress semantics. |
| `TrailsSurface`, `TrailsCard` | [Surface][surface], [Card][card] | Shared rounded surfaces for image, statistics, collection and settings cards. `SurfaceVariant` provides `Default`, `Secondary`, `Tertiary`, and `Transparent`. |
| `TrailsChip` | [Chip][chip] | Informational labels and status pills. `ChipTone` provides `Default`, `Accent`, `Success`, `Warning`, and `Danger`; `ChipSize` provides `Small`, `Medium`, and `Large`. |
| `TrailsListGroup`, `TrailsListItem` | [ListGroup][list-group] | Group surface and row spacing with leading content, body and independent trailing actions. Checkbox rows use the separate ControlField adaptation. |
| `TrailsSeparator` | [Separator][separator] | Horizontal or vertical hairline separators for lists, facts and settings. |
| `TrailsBottomSheet` | [BottomSheet][bottom-sheet] | Overlay surface, 32 dp top corners and handle. Native sheet state owns gestures, insets, Back and guarded dismissal. |
| `TrailsAlert` | [Alert][alert] | Rounded feedback surface with status-colored content and a separate recovery action when available. |
| `TrailsStatusLine` | [Chip][chip], [ControlField][control-field], [Button][button], [Alert][alert] | Pending, offline and informational messages compose status content and optional actions. Failed and attention states use an alert. Status meaning comes from repository state. |
| `TrailsToast`, `TrailsToastHost` | [Toast styles][toast], [motion][toast-motion] | Overlay appearance and motion with Trails' five-second lifecycle, optional action, polite announcement and tap interception. |
| `TrailsFloatingNav` | [Tabs][tabs] and [Surface][surface] | Navigation composition for five independent roots. Preserve selected state, callbacks, insets and readable wrapping at large text sizes. |
| `TrailsModalDrawer` | [Dialog][dialog] and [Surface][surface] | Dialog overlay styling composed with a native side-drawer shell. Side entry, edge swipe, Back/Escape and account lifetime remain native drawer behavior. |

Native layout may enlarge interaction bounds or let labels grow beyond a
reference's fixed visual size. Targets remain at least 48 dp. A smaller thumb,
checkbox, link or icon must not reduce its target. Preserve labels, roles,
selection/checked state, disabled state and keyboard focus on the actionable
node. Nested bookmark actions remain separate from opening a card.

Use the pinned motion definitions rather than generic Material animation.
PressableFeedback's default scale is 0.985 before width adjustment, with a
300 ms transition. Respect disabled system animations and provide a visible
static state and focus indication without motion. Native sheet and drawer
transitions remain owned by their behavioral shells. The drawer's side entry and the
navigation's wrapping layout are explicit compositions, not literal Native
Dialog positioning or Tabs layout. Bottom navigation uses compact 12/16 sp labels
and 4 dp horizontal trigger padding instead of the reference Tabs' 16/24 sp and
12 dp padding, keeping all five destinations visible at narrow widths and 200%
text while retaining 48 dp targets.

Select uses a Compose `Popup` for window focus and dismissal. Its explicit
animation follows the pinned Select popover: a 200 ms fade/translation with
scale from 0.97 to 1 on entry, and a 150 ms fade/translation without shrinking
on exit. Entry waits until the popup fits its resolved position. The popup
uses the [Native defaults][select-defaults] of an 8 dp anchor gap/translation
and 12 dp viewport insets. It centers on the trigger, flips above it when needed,
and constrains a scrollable option list to the viewport. Selected-option focus, arrow-key
navigation, Escape/Back/outside dismissal and focus return remain native Compose
behavior. Disabling the trigger closes the popup and blocks option activation
during exit. Disabled system animations resolve directly to the static state.
This adaptation uses the popover presentation; Native's dialog and bottom-sheet
Select presentations are not exposed by `TrailsSelect`.

## Named compositions and consumers

The adapter names above are the visual reference for these wrappers. Follow the
linked source when changing a composition: a wrapper can add domain state,
copy, layout or callbacks without becoming a new upstream component. States
below describe supported presentation, not a claim that every state has device
verification.

| Reusable name and source | Consumers | Adapter composition and supported states |
| --- | --- | --- |
| [`TrailsControlsButton`][controls-source] | Welcome, Collection empty state | Compatibility wrapper over `TrailsButton`: Commit or Secondary tone, enabled/disabled, loading, pressed and focused. Loading blocks activation. |
| [`TrailsIconCircle`, `TrailsIconButton`][button-source] | Bookmark, Collection/Detail Back, Filters close, search clear, Developer tools close | Button icon variants. Circle adds the surface/shadow; IconButton is transparent. Both retain one labeled action and pressed/focused feedback. Bookmark state belongs to `TrailBookmark`. |
| [`TrailsLoading`, `TrailsHeading`][feedback-source] | Startup, signed-in screen loading/recovery states, Save Trail headings/loading | `TrailsSpinner` plus body text for indeterminate loading. Heading is typography with heading semantics. Neither is an interactive control. |
| [`TrailSaveCard`][trail-save-source] | Explore, Saved all-trails, Collection | `TrailsCard` + `TrailPhoto`, `HighlightChip`, `TrailBookmark`, `TrailFactsRow` and optional `TrailsStatusLine`. Card navigation and bookmark actions stay independent. Saved membership may be known or unknown; sync feedback follows the snapshot. |
| [`HighlightChip`][trail-save-source] | `TrailSaveCard` | Small informational `TrailsChip` with a decorative icon, present only when a trail has a highlight. No selection or click state. |
| [`TrailBookmark`][trail-save-source] | `TrailSaveCard`, For You recommendation rows, Activity cards/rows, Trail Detail | `TrailsIconCircle` with saved, not-saved and unavailable-membership semantics. Pressed/focused feedback and focus return after Save Trail dismissal. Unknown membership is not rendered as a confirmed unsaved state. |
| [`TrailFactsRow`][trail-save-source] | `TrailSaveCard`, For You recommendation rows, Trail Detail | Wrapping typography/icon primitives and the domain `DifficultyMarker`, announced as one facts line. Review count is optional; trailing text is supplied by the caller. No interactive states. |
| [`TrailPhotoCredit`][photo-source] | Welcome, Trail Detail | Typography plus `TrailsLinkButton` for source/license destinations. Omitted when no photograph metadata exists. Links retain pressed/focused feedback. The photograph itself is domain content. |
| [`TrailSyncNotice`, `SavedSyncNotice`][sync-source] | Trail Detail; Saved and Collection summaries | `TrailsStatusLine` driven by `syncStatus`: pending/offline, syncing, finishing, retryable failure and parked/attention. Settled or absent sync produces no notice. Retry appears only for retryable status while not already syncing. |
| [`TrailsStatusLine`, `StatusIndicator`][status-source] | Screen/feature recovery and sync notices, runtime checkpoint failure | Chip or Alert with a decorative status icon and optional ghost `TrailsTextAction`. Pending, offline, info, failed and attention states keep their domain meaning. An action needs both a label and callback. |
| [`TrailsToast`, `TrailsToastHost`][status-source] | Save Trail completion, Navigate recording placeholder | Toast surface, optional check/action and host enter/exit motion. Absent/present states, five-second dismissal and polite announcement. Navigate positions its toast above the persistent panel; the account shell positions save completion above navigation. |
| [`DeveloperToolsDrawerHost`][developer-host-source] | Runtime Welcome, prelanding and active-account shells | `TrailsModalDrawer` + branding, icon action, tabs, settings and feedback below. Open/closed, runtime-unavailable, applying/applied and operation/runtime-error states. Native focus, dismissal and graph lifetime stay with the host. |
| [`DeveloperToolsTabs`, `DeveloperToolsStatus`, `DeveloperToolsNotice`][developer-controls-source] | Developer tools drawer | `TrailsSegmentedControl` for Network/Sync/Session; `TrailsChip` for Settings/Applying/Applied/Failed/Unavailable; `TrailsAlert` for informational/error notices. Status changes are announced politely. |
| [`SettingsGroup`, `SettingsDivider`][developer-controls-source] | Developer tools sections | `TrailsListGroup` and inset `TrailsSeparator`. Structural presentation without interactive state. |
| [`SettingsToggle`, `SettingsSlider`, `SettingsSelect`][developer-controls-source] | Developer tools Network/Sync settings | `TrailsSwitchRow`, `TrailsSlider` and `TrailsSelect` with labels/hints. Checked/selected, enabled/disabled and focused/pressed states as applicable. Sliders clamp/quantize draft values and invoke persistence on gesture completion. |

Screen-local compositions are included because replacing a shared adapter must
also preserve their state and interaction contracts.

| Composition and source | Consumer | Adapter composition and supported states |
| --- | --- | --- |
| [`SortMenu`][explore-source] | Explore | Compact `TrailsSelect`: closed/open, selected sort, keyboard focus and dismissal. |
| [`SavedTabs`, `CollectionTile`, `SavedEmpty`][saved-source] | Saved | Tabs select Lists/All trails. Tiles compose `TrailsCard` with a photo or Secondary `TrailsSurface` placeholder and adapt columns to available width/text size. Empty presentation composes Surface/typography/Button and differs from unknown membership or missing details. |
| [`FeatureCard`, `RecommendationRow`][for-you-source] | For You | Featured `TrailsCard` disables navigation when trail details are missing. Rows compose `TrailsListGroup`/`TrailsListItem`, bookmark, facts, sync status and separator. Missing recommendations are omitted and receive separate recovery feedback. |
| [`MonthCard`, `MonthStat`, `ActivityHero`, `ActivityRow`][activity-source] | Activity | Summary Card and typography contain the domain chart. Hero/rows use Card or ListGroup/ListItem plus photo, bookmark, sync status and separator. Missing catalog details omit the bookmark while preserving activity navigation by trail ID. Empty and failed history remain distinct. |
| [`TrailNamePill` and persistent panel][navigate-source] | Navigate | Pill composes `TrailsPressable`/`TrailsSurface` only when a trail is available. Loading or failure replaces it. Panel composes Surface/typography/Button and remains nonmodal; its measured height also positions the schematic and toast. |
| [`TrailFacts`, `TrailFact`, `ExpandableDescription`, `TrailTag`][detail-source] | Trail Detail | Responsive fact typography with `TrailsSeparator`; collapsed/expanded description with ghost `TrailsTextAction`; informational `TrailsChip` tags. Back/bookmark/save/retry use their shared adapters. |
| [`RealFiltersFeature.Content`, `SectionHeading`, `MaximumSlider`][filters-source] | Explore filter triggers | `TrailsBottomSheet` with heading typography, Slider, SwitchRow, FilterChip, CheckboxRow and Button. Selected draft values, bounds and count/loading/error states remain feature-owned. MaximumSlider maps its upper stop to no maximum. |
| [`RealSaveTrailFeature.Content`, `RealSaveTrailFeature.Toast`][save-source] | Cards/rows/Trail Detail via the account shell | BottomSheet + CheckboxRow, Loading, StatusLine and Button; ToastHost on completion. Loading/unavailable, editable, admitting, reconciling, unknown, failed, confirm-remove and journaled states retain their dismissal/editability guards. |
| [`StartupSplashContent`, `StartupFailureContent`][startup-source] | `MainViewController` | Brand plus Loading or Alert, and failure StatusLine/Button. Startup loading, backend-configuration failure and bootstrap failure keep the original retry callbacks. |
| [`NavigationCheckpointFailure`][account-source] | `AccountContent` | Failed StatusLine plus Retry. The account shell separately composes `TrailsFloatingNav`, screen content and feature overlays; checkpoint failure does not replace the current screen or stack. |

`TrailsSwitchIndicator` is the private visual part of `TrailsSwitchRow`.
`TrailsSelectPopupPositionProvider` belongs to Select's popup placement.
`trailsShadow`, `surfaceColor` and the private `surface` modifier belong to the
Surface/Card styling implementation. These are parts of the mapped adapters.
`TrailsTheme`, `trailsMotionEnabled`, `rememberTrailsFocusReturnTarget`,
`TrailsFocusReturnTarget`, `rememberCheckpointedListState`,
`rememberCheckpointedScrollState` and `CheckpointedScrollState` provide theme,
focus and scroll infrastructure, not additional visual component families.

The named domain exceptions are `TrailsCompass`/`TrailsBrand`,
`DifficultyMarker`, `TrailPhoto`, `TrailRouteSchematic`/`schematicRoute` and
Activity's `LastSevenDaysBars`. Their assets/drawing and content semantics stay
local. `TrailPhoto` can render a Photo unavailable text fallback; that does not
turn the photograph into an upstream component. Formatting helpers such as
`trailDistance`, `trailDuration`, `markerKind` and `syncStatus` provide content or
state to the compositions above.

## Screen and feature inventory

Review the whole surface when changing a shared adapter. A screen can contain
both adapted components and domain content; sharing a name or color does not
establish adaptation coverage.

| Surface | Components and compositions to account for |
| --- | --- |
| Welcome | Hero image card, branding, photograph links, loading action and failure alert. |
| Explore | Search, selectable filter triggers, sort select, trail cards and highlights, loading, empty and recovery states. |
| For You | Featured image card, recommendation list rows, bookmark actions, separators, loading and unavailable/refresh states. |
| Navigate | Trail-name pressable row, schematic-label chip, persistent surface panel, recording-placeholder action, toast and load/recovery states. The panel is not modal. |
| Saved | Lists/All trails tabs, collection tiles and placeholders, trail cards, loading, empty, missing-details and sync states. |
| Activity | Summary card, activity hero card, list rows, bookmarks, separators, loading, empty and recovery states. The chart is domain content. |
| Collection | Back action, trail cards, collection counts, sync notices, empty, unavailable and missing-details states. |
| Trail Detail | Photo and overlay actions, content surface, fact separators, display tags, expandable description action, photograph links, save action and recovery/sync states. |
| Filters | Modal sheet, sliders, switch, selectable tags, checkbox rows, count feedback and apply/cancel actions. |
| Save Trail | Guarded modal sheet, collection checkbox rows, loading, admission/reconciliation/confirmation states, actions and completion toast. |
| Developer tools | Drawer shell, tabs, status chip, settings groups, separators, controls, recovery, copy/reset and session actions. |
| Runtime and account shell | Startup splash, configuration restoration, startup failure and retry, navigation, navigation-checkpoint failure, feature overlays and toast placement. |

Empty and unavailable presentations compose Surface, typography, Spinner,
Alert and Button as appropriate. Do not label a composition as a separate
upstream component or add a Pro dependency to name it.

The original compass/wordmark, photographs, difficulty symbols, route schematic
and activity chart are domain assets or drawings. Retain them inside the shared
surfaces without calling the drawings HeroUI components. Plain text, icons,
layout, scrolling, insets, canvas drawing and focus/state infrastructure are
primitives. Typography roles and component geometry still apply to their use
inside a component.

## Behavior boundaries

Visual adapters receive state and callbacks. Keep domain behavior in features,
presenters and repositories:

- Filters own the draft, inclusive bounds, upper-stop “no maximum” meaning and
  query-count identity. Dismissal discards the draft.
- Developer settings persist slider edits when gestures finish. Applied status
  must match the runtime-applied configuration, not just a completed local write.
- Save Trail supplies its admission/reconciliation dismissal guard and retains
  unknown membership, editable draft and frozen admitted payload distinctions.
- Cards and rows preserve separate open-trail and bookmark actions, focus return,
  disabled missing-data states, and scroll checkpoints.
- Navigation preserves all five root stacks. Native sheets and the drawer retain
  modal isolation, Back handling and insets. Navigate's persistent panel does not
  acquire modal behavior.
- Toast styling does not change its completion wording, collection destination,
  callback, timeout or positioning above navigation and persistent panels.

## Enforcement and verification

Run `python3 scripts/check_components.py` and its regression fixtures using
`python3 -m unittest discover -s scripts/testing -p test_components.py`.
The source check rejects known direct Material visual-control imports and uses
outside the design system, including import aliases, wildcard uses and fully
qualified calls. It permits native state APIs, annotations, Text and Icon.
Layout-only `Scaffold` is permitted only in `app/runtime`'s `AccountContent.kt`.
Tests and generated build output are excluded.

This is a lexical boundary check, not Kotlin symbol resolution or visual proof.
It masks comments and string literals, recognizes common local declarations,
and cannot establish coverage for custom surfaces, arbitrary wrappers or
interpolated string expressions. Passing it does not prove that every component
matches HeroUI. Review the inventory and each adapter's normal, pressed,
focused, selected, disabled and loading states where applicable.

Use the [testing guide](testing.md#component-verification) for the installed
screen/state matrix, motion and accessibility checks. Keep source-reference,
local-test, Android-build and installed-device evidence distinct.

[reference]: https://github.com/heroui-inc/heroui-native/blob/122f63db3159f2192a9de38c0e85e5ae9004473a/package.json
[variables]: https://github.com/heroui-inc/heroui-native/blob/122f63db3159f2192a9de38c0e85e5ae9004473a/src/styles/variables.css
[theme]: https://github.com/heroui-inc/heroui-native/blob/122f63db3159f2192a9de38c0e85e5ae9004473a/src/styles/theme.css
[slider]: https://github.com/heroui-inc/heroui-native/blob/122f63db3159f2192a9de38c0e85e5ae9004473a/src/styles/components/slider.css
[slider-behavior]: https://github.com/heroui-inc/heroui-native/blob/122f63db3159f2192a9de38c0e85e5ae9004473a/src/components/slider/slider.tsx
[switch]: https://github.com/heroui-inc/heroui-native/blob/122f63db3159f2192a9de38c0e85e5ae9004473a/src/styles/components/switch.css
[control-field]: https://github.com/heroui-inc/heroui-native/blob/122f63db3159f2192a9de38c0e85e5ae9004473a/src/styles/components/control-field.css
[checkbox]: https://github.com/heroui-inc/heroui-native/blob/122f63db3159f2192a9de38c0e85e5ae9004473a/src/styles/components/checkbox.css
[tag-group]: https://github.com/heroui-inc/heroui-native/blob/122f63db3159f2192a9de38c0e85e5ae9004473a/src/styles/components/tag-group.css
[tabs]: https://github.com/heroui-inc/heroui-native/blob/122f63db3159f2192a9de38c0e85e5ae9004473a/src/styles/components/tabs.css
[tabs-motion]: https://github.com/heroui-inc/heroui-native/blob/122f63db3159f2192a9de38c0e85e5ae9004473a/src/components/tabs/tabs.animation.ts
[select]: https://github.com/heroui-inc/heroui-native/blob/122f63db3159f2192a9de38c0e85e5ae9004473a/src/styles/components/select.css
[select-behavior]: https://github.com/heroui-inc/heroui-native/blob/122f63db3159f2192a9de38c0e85e5ae9004473a/src/components/select/select.tsx
[select-popup-motion]: https://github.com/heroui-inc/heroui-native/blob/122f63db3159f2192a9de38c0e85e5ae9004473a/src/helpers/internal/hooks/use-popup-popover-content-animation.ts
[select-indicator-motion]: https://github.com/heroui-inc/heroui-native/blob/122f63db3159f2192a9de38c0e85e5ae9004473a/src/components/select/select.animation.ts
[select-defaults]: https://github.com/heroui-inc/heroui-native/blob/122f63db3159f2192a9de38c0e85e5ae9004473a/src/components/select/select.constants.ts
[search-field]: https://github.com/heroui-inc/heroui-native/blob/122f63db3159f2192a9de38c0e85e5ae9004473a/src/styles/components/search-field.css
[input]: https://github.com/heroui-inc/heroui-native/blob/122f63db3159f2192a9de38c0e85e5ae9004473a/src/styles/components/input.css
[button]: https://github.com/heroui-inc/heroui-native/blob/122f63db3159f2192a9de38c0e85e5ae9004473a/src/styles/components/button.css
[button-behavior]: https://github.com/heroui-inc/heroui-native/blob/122f63db3159f2192a9de38c0e85e5ae9004473a/src/components/button/button.tsx
[link-button]: https://github.com/heroui-inc/heroui-native/blob/122f63db3159f2192a9de38c0e85e5ae9004473a/src/components/link-button/link-button.tsx
[pressable]: https://github.com/heroui-inc/heroui-native/blob/122f63db3159f2192a9de38c0e85e5ae9004473a/src/components/pressable-feedback/pressable-feedback.tsx
[pressable-motion]: https://github.com/heroui-inc/heroui-native/blob/122f63db3159f2192a9de38c0e85e5ae9004473a/src/components/pressable-feedback/pressable-feedback.animation.ts
[spinner]: https://github.com/heroui-inc/heroui-native/blob/122f63db3159f2192a9de38c0e85e5ae9004473a/src/components/spinner/spinner-icon.tsx
[spinner-constants]: https://github.com/heroui-inc/heroui-native/blob/122f63db3159f2192a9de38c0e85e5ae9004473a/src/components/spinner/spinner.constants.ts
[surface]: https://github.com/heroui-inc/heroui-native/blob/122f63db3159f2192a9de38c0e85e5ae9004473a/src/styles/components/surface.css
[card]: https://github.com/heroui-inc/heroui-native/blob/122f63db3159f2192a9de38c0e85e5ae9004473a/src/styles/components/card.css
[chip]: https://github.com/heroui-inc/heroui-native/blob/122f63db3159f2192a9de38c0e85e5ae9004473a/src/styles/components/chip.css
[list-group]: https://github.com/heroui-inc/heroui-native/blob/122f63db3159f2192a9de38c0e85e5ae9004473a/src/styles/components/list-group.css
[separator]: https://github.com/heroui-inc/heroui-native/blob/122f63db3159f2192a9de38c0e85e5ae9004473a/src/styles/components/separator.css
[bottom-sheet]: https://github.com/heroui-inc/heroui-native/blob/122f63db3159f2192a9de38c0e85e5ae9004473a/src/styles/components/bottom-sheet.css
[alert]: https://github.com/heroui-inc/heroui-native/blob/122f63db3159f2192a9de38c0e85e5ae9004473a/src/styles/components/alert.css
[toast]: https://github.com/heroui-inc/heroui-native/blob/122f63db3159f2192a9de38c0e85e5ae9004473a/src/styles/components/toast.css
[toast-motion]: https://github.com/heroui-inc/heroui-native/blob/122f63db3159f2192a9de38c0e85e5ae9004473a/src/components/toast/toast.animation.ts
[dialog]: https://github.com/heroui-inc/heroui-native/blob/122f63db3159f2192a9de38c0e85e5ae9004473a/src/styles/components/dialog.css
[controls-source]: ../multiplatform/foundation/designsystem/src/commonMain/kotlin/org/mobilenativefoundation/trails/foundation/designsystem/component/TrailsControls.kt
[button-source]: ../multiplatform/foundation/designsystem/src/commonMain/kotlin/org/mobilenativefoundation/trails/foundation/designsystem/component/TrailsButton.kt
[feedback-source]: ../multiplatform/foundation/designsystem/src/commonMain/kotlin/org/mobilenativefoundation/trails/foundation/designsystem/component/ContentFeedback.kt
[status-source]: ../multiplatform/foundation/designsystem/src/commonMain/kotlin/org/mobilenativefoundation/trails/foundation/designsystem/component/TrailsStatus.kt
[trail-save-source]: ../multiplatform/ui/trail/src/commonMain/kotlin/org/mobilenativefoundation/trails/ui/trail/TrailSaveCard.kt
[photo-source]: ../multiplatform/ui/trail/src/commonMain/kotlin/org/mobilenativefoundation/trails/ui/trail/TrailPhoto.kt
[sync-source]: ../multiplatform/ui/trail/src/commonMain/kotlin/org/mobilenativefoundation/trails/ui/trail/TrailSyncNotice.kt
[developer-host-source]: ../multiplatform/feature/developertools/impl/src/commonMain/kotlin/org/mobilenativefoundation/trails/feature/developertools/DeveloperToolsDrawerHost.kt
[developer-controls-source]: ../multiplatform/feature/developertools/impl/src/commonMain/kotlin/org/mobilenativefoundation/trails/feature/developertools/DeveloperToolsControls.kt
[explore-source]: ../multiplatform/screen/explore/impl/src/commonMain/kotlin/org/mobilenativefoundation/trails/screen/explore/ExploreUi.kt
[saved-source]: ../multiplatform/screen/saved/impl/src/commonMain/kotlin/org/mobilenativefoundation/trails/screen/saved/SavedUi.kt
[for-you-source]: ../multiplatform/screen/foryou/impl/src/commonMain/kotlin/org/mobilenativefoundation/trails/screen/foryou/ForYouUi.kt
[activity-source]: ../multiplatform/screen/activity/impl/src/commonMain/kotlin/org/mobilenativefoundation/trails/screen/activity/ActivityUi.kt
[navigate-source]: ../multiplatform/screen/navigate/impl/src/commonMain/kotlin/org/mobilenativefoundation/trails/screen/navigate/NavigateUi.kt
[detail-source]: ../multiplatform/screen/traildetail/impl/src/commonMain/kotlin/org/mobilenativefoundation/trails/screen/traildetail/TrailDetailUi.kt
[filters-source]: ../multiplatform/feature/filters/impl/src/commonMain/kotlin/org/mobilenativefoundation/trails/feature/filters/RealFiltersFeature.kt
[save-source]: ../multiplatform/feature/savetrail/impl/src/commonMain/kotlin/org/mobilenativefoundation/trails/feature/savetrail/RealSaveTrailFeature.kt
[startup-source]: ../multiplatform/app/runtime/src/commonMain/kotlin/org/mobilenativefoundation/trails/app/runtime/MainViewController.kt
[account-source]: ../multiplatform/app/runtime/src/commonMain/kotlin/org/mobilenativefoundation/trails/app/runtime/AccountContent.kt
