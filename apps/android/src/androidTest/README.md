# Android accessibility instrumentation

`TrailsAccessibilityInstrumentation` uses Android framework automation to check
native control semantics, modal traversal, TalkBack sheet opening, and focus
return. The Gradle runner setting supplies the test manifest entry.

## Run

Start with an active sample account, a readable saved projection, and an Explore
query containing **Half Dome** and the **Weekend adventures** and **Favorites**
lists. Use default length bounds: 0 km and no maximum (50 on the native range).
TalkBack must be enabled, bound, and providing touch exploration with its default
previous/next and double-tap mappings. Close TalkBack onboarding first. Do not
run another automation connection or `uiautomator dump` concurrently.

```bash
./gradlew :apps:android:assembleDebug :apps:android:assembleDebugAndroidTest
adb install -r apps/android/build/outputs/apk/debug/android-debug.apk
adb install -r apps/android/build/outputs/apk/androidTest/debug/android-debug-androidTest.apk
adb shell am instrument -w -r org.mobilenativefoundation.trails.android.test/org.mobilenativefoundation.trails.android.accessibility.TrailsAccessibilityInstrumentation
```

Select the intended device with `adb -s <serial>` when several devices are
connected. For the separate Offline preview application, build both APKs with
the same preview property and use that test application ID. Preserve complete
command output, including the first failure.

The runner uses
`getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)` and
checks the spoken-feedback service, touch exploration, and bound-service state
before launch and after each sheet. It allows 30 seconds for recognized app
content, polling every 250 ms. Individual control waits allow eight seconds.
Welcome or bootstrap failure stops the run; an unknown startup window never
receives Back.

## Gesture and focus checks

Sheet openers use the Android test method
`UiAutomation.injectInputEventToInputFilter(InputEvent)`. Ordinary
`injectInputEvent` skips the accessibility input filter and cannot establish
TalkBack gesture behavior. The runner fails if the test method is unavailable;
it does not substitute direct focus. If hidden-API enforcement blocks it, run
the changed instrumentation command with `--no-hidden-api-checks`, preserving
the original failure. Do not change global hidden-API settings.

Each opener allows at most 24 gestures within 30 seconds, including activation
and modal readiness. A gesture must produce a new app-window
`TYPE_VIEW_ACCESSIBILITY_FOCUSED` event within three seconds. Before activation,
the runner requires `TYPE_TOUCH_INTERACTION_END` followed by 500 ms of event
quiet. Each synchronization phase is bounded by five seconds and the opener's
overall deadline. Zero gestures is valid only when the invoker already owns
accessibility focus.

Inside a sheet, direct `ACTION_ACCESSIBILITY_FOCUS` traversal verifies focus
ownership and rejects exposed actionable background controls. Modal readiness
requires 500 ms of event quiet, bounded by five seconds, and existing focus in
the modal before any focus request. The Filters sheet exposes the **Filters**
heading and **Close filters** action. Its **Minimum length** and **Maximum length**
SeekBars must expose range actions and updated kilometer values after
`ACTION_SET_PROGRESS`. A maximum of 50 exposes **No maximum length**.
Cancelling and reopening verifies that draft adjustments did not change applied
filters or saved membership. The runner does not submit a save, change backend
settings, or clear app data.

After filter Cancel/Back and save-sheet Back, natural focus return is observed
for up to eight seconds before any further forced focus or navigation. Focus
must return to the actual opener or its non-actionable label/role descendant.
An enclosing ancestor or another actionable control cannot satisfy the check.
The save sheet is identified by **Save to a list**. Trail detail is identified
by its persistent **Save trail** or **Edit saved collections** action after the
sheet closes. Final Saved navigation sends Back only from a recognized
detail/collection and stops at the **Lists**/**All trails** root. Selected-tab
checks inspect every matching label because the page heading also says **Saved**.

## Results and limits

`TRAILS_A11Y` output records native focus/click/progress observations, service
checks, node semantics, and the last 200 relevant events. The terminal bundle
must contain `outcome=passed`; shell instrumentation completion alone is
insufficient. A failed run includes the first failure and native tree. Startup
probes distinguish cached automation trees from fresh reads and inspect an
already-created app graph without creating services or retrying bootstrap.

Passing establishes the measured native actions and observed TalkBack gesture
openers. It does not establish human listening, pronunciation, or complete
TalkBack swipe traversal through every sheet. The instrumentation may restart
the app process while preserving its data. Restore device settings after the
run and report the exact APK and test package used.

The Android host has a known focus-return limitation: cancelling Filters can
return TalkBack focus to Search even though keyboard focus returns to Filters.
The runner reports this as a failure; input focus alone does not satisfy the
accessibility assertion. Later checks are unexecuted when this assertion fails.
