# Application behavior

## Navigation and sample content

Explore, For You, Navigate, Saved, and Activity retain independent back stacks.
Returning from trail detail restores its originating root, query, selected
segment, and scroll position. Selecting the current root does not push another
root. Account checkpoints preserve view state; they are separate from domain
storage.

For You reads a seeded recommendation feed. Activity reads seeded per-account
activities and derives its totals from those rows in the device time zone.
Activity rows open their trail details. Both feeds share the same saved
membership and synchronization status as Explore. Welcome identifies ratings,
reviews, activity history, and recommendations as sample data.

Navigate chooses the last-opened trail, then the first saved trail, then Half
Dome. Its route illustration is labelled **Route preview · schematic** and
contains no basemap or measured geometry. **Start recording** displays
**Recording is coming soon**. There is no recorder, location tracking,
personalization, map download, sharing, or collection-management workflow.

## Search and filters

Search matches trail name and region without case sensitivity. Query identity
trims outer whitespace and collapses internal whitespace; the editable text is
kept separately. Search input is debounced, while IME Search submits immediately.
Clearing text preserves filters; clearing filters preserves text. A new query
resets result scroll, while returning from detail restores it.

Difficulty and activity selections match any selected value within their group.
Selected features must all match. Text, difficulty, length, elevation gain,
features, and activity combine with AND. An empty selection imposes no constraint.
Distances and elevation use canonical metres with inclusive bounds. The upper
range stop means no maximum. Region has no independent control; old checkpointed
region filters are discarded on restore.

Most popular preserves catalog order. Highest rated, Shortest, and Longest reorder
the matching trails. Counts come from a successful query, never from a loading
or failure placeholder. A late result from an older query cannot replace the
current query's content or count. Refresh retains available content.

Filters edit a copy of the applied query. Dismissal discards the draft. Applying
uses the exact draft represented by the displayed count. While counting, the
count action is disabled and Cancel remains available. Count failure permits
applying without a claimed count and retrying the count separately.

## Saving and synchronization

The save sheet waits for both account collections and the trail's current
membership. Unknown membership is not an empty set. A failed read offers Retry
and Cancel without enabling submission; a successful local read can establish
readiness offline.

Selections remain a draft until submission. Submission freezes the collection
set and application command identity. A repeated identity cannot describe a
different payload. The adapter records its application receipt with durable
mutation admission. Store6's pinned
[`MutationStore.mutate`](https://github.com/MobileNativeFoundation/Store/blob/582edfe86e64ddc71312ecd20a1895fc3de37b52/mutations/src/commonMain/kotlin/org/mobilenativefoundation/store6/mutations/MutationStore.kt)
assigns the mutation ID; application command IDs and transport idempotency keys
have separate roles.

The sheet closes after durable local admission, without waiting for the network.
The completion toast offers the sole selected collection or Saved for multiple
collections. Known non-admission retains editable choices. An uncertain outcome
keeps the submitted payload frozen while reconciliation checks that same command;
it must not blindly enqueue another mutation. A later status-refresh failure
cannot turn completed admission into a failed save.

The account service owns draining, retry, acknowledgement, and local adoption.
Pending, retryable, parked, and settled states come from persisted repository
state. Parked incompatible work has no misleading Retry action. A lost remote
acknowledgement reuses the operation identity so the fake backend does not apply
the same operation twice. The backend commits its authoritative change and
operation receipt together.

Removing a trail from one collection preserves its other memberships. Removing
all memberships requires the explicit removal action. Cancelling an unsubmitted
draft has no data effect; dismissing an admitted operation does not roll it back.

## Account and failure boundaries

Restore the persisted session and applied developer settings before opening
account repositories or starting network work. Account retirement removes private
UI, cancels and joins its work, and closes its drivers before another account
opens. Sign-out preserves pending work in the old account partition without
exposing it in another account.

Known-empty, unavailable, and not-yet-loaded states remain distinct. A missing
trail displays its own unavailable state rather than another trail's facts.
Refresh failure retains readable content and exposes recovery. Cold offline
access cannot promise data that is absent from local storage.

Developer **Offline** controls the fake backend. It is independent of airplane
mode. Settings distinguish a pending edit from the configuration actually
applied by the backend; restart restores that applied configuration.

## Design reference

The [Trails Figma file](https://www.figma.com/design/4B7GK9ndPVQ1BFIKGg0Zqj/Trails?node-id=153-97)
is the visual reference. The app uses native Compose controls, Android insets,
keyboard behavior, and accessibility semantics. The original compass remains the
brand mark. Bundled assets and their licenses are documented alongside the
[design system](../multiplatform/foundation/designsystem/ASSETS.md).

Controls must remain readable and operable at 200% font scale with at least
48 dp touch targets. Modal traversal must not expose actionable background
content. Automated semantics checks, observed TalkBack gestures, and human
listening establish different coverage; none substitutes for the others.
