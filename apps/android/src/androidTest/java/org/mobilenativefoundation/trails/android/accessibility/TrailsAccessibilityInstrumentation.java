package org.mobilenativefoundation.trails.android.accessibility;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.app.Instrumentation;
import android.app.UiAutomation;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.InputEvent;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mobilenativefoundation.trails.android.App;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/** Platform-only acceptance runner. Never suppresses TalkBack and never submits a save. */
public final class TrailsAccessibilityInstrumentation extends Instrumentation {
    private static final long WAIT_MS = 8_000;
    private static final long STARTUP_WAIT_MS = 30_000;
    private static final long OPENER_GESTURE_WAIT_MS = 30_000;
    private static final int MAX_OPENER_GESTURES = 24;
    private static final int SET_PROGRESS = AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS.getId();
    private static final String MINIMUM = "Minimum length";
    private static final String MAXIMUM = "Maximum length";
    private static final float MAXIMUM_LENGTH_KM = 50;
    private static final String TRAIL_NAME = "Half Dome";
    private final List<String> events = new ArrayList<>();
    private UiAutomation automation;
    private Method injectInputFilter;
    private String targetPackage;
    private volatile boolean startupReady;
    private volatile long lastTouchInteractionStartTime;
    private volatile long lastTouchInteractionEndTime;
    private volatile long lastInjectedSwipeStartTime;
    private boolean graphBackedUiObserved;
    private long launchStartedAt;
    private int checks;

    @Override public void onCreate(Bundle arguments) {
        super.onCreate(arguments);
        start();
    }

    @Override public void onStart() {
        Bundle result = new Bundle();
        try {
            targetPackage = getTargetContext().getPackageName();
            automation = getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES);
            automation.setOnAccessibilityEventListener(event -> {
                int type = event.getEventType();
                boolean touchInteraction = type == AccessibilityEvent.TYPE_TOUCH_INTERACTION_START ||
                        type == AccessibilityEvent.TYPE_TOUCH_INTERACTION_END;
                // Framework touch-interaction events have no package. Keep their timestamps
                // to distinguish a focus event mid-gesture from completed gesture delivery.
                if (type == AccessibilityEvent.TYPE_TOUCH_INTERACTION_START) {
                    lastTouchInteractionStartTime = Math.max(lastTouchInteractionStartTime, event.getEventTime());
                } else if (type == AccessibilityEvent.TYPE_TOUCH_INTERACTION_END) {
                    lastTouchInteractionEndTime = Math.max(lastTouchInteractionEndTime, event.getEventTime());
                }
                if (!touchInteraction && !targetPackage.contentEquals(event.getPackageName() == null ? "" : event.getPackageName())) return;
                if (!touchInteraction && type != AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED &&
                        type != AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED &&
                        type != AccessibilityEvent.TYPE_VIEW_FOCUSED &&
                        type != AccessibilityEvent.TYPE_VIEW_CLICKED &&
                        type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
                        type != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) return;
                synchronized (events) {
                    // Preserve dismissal and failure ordering rather than only startup events.
                    if (events.size() == 200) events.remove(0);
                    events.add(eventRecord(event, startupReady).toString());
                }
            });
            requireTalkBack("before_launch");
            resolveInputFilterInjection();
            Intent launch = getTargetContext().getPackageManager().getLaunchIntentForPackage(targetPackage);
            require(launch != null, "Target has a launcher activity");
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            launchStartedAt = SystemClock.uptimeMillis();
            getTargetContext().startActivity(launch);
            awaitInitialAppReady();
            startupReady = true;
            record("launch_to_ready", "durationMs", Long.toString(SystemClock.uptimeMillis() - launchStartedAt),
                    "deadlineMs", Long.toString(STARTUP_WAIT_MS));
            record("initial_ready_tree", "nodes", treeRecord(appRoot()).toString());
            openExplore();
            checkFilters();
            requireTalkBack("after_filters");
            checkSaveSheet();
            requireTalkBack("after_save_sheet");
            result.putString("stream", "\nTrails accessibility checks passed: " + checks +
                    ". Native actions and input-filter opener gestures; spoken audio and full TalkBack sheet traversal were not evaluated.\n");
            result.putString("outcome", "passed");
        } catch (Throwable failure) {
            result.putString("outcome", "failed");
            result.putString("stream", "\nTrails accessibility FIRST FAILURE: " + failure + "\n");
            result.putString("failure", failure.toString());
            try {
                AccessibilityNodeInfo root = appRoot();
                recordGraphSnapshot("failure", root);
                record("failure_tree", "nodes", treeRecord(root).toString());
            }
            catch (Throwable ignored) { /* Preserve original failure. */ }
        } finally {
            synchronized (events) {
                for (String event : events) record("native_event", "event", event);
            }
            result.putInt("checks", checks);
            if (automation != null) automation.setOnAccessibilityEventListener(null);
            finish("passed".equals(result.getString("outcome")) ? Activity.RESULT_OK : Activity.RESULT_CANCELED, result);
        }
    }

    private void checkFilters() throws Exception {
        AccessibilityNodeInfo cancelInvoker = findOrScroll("Filters", true);
        openWithTalkBack(cancelInvoker, "filters_initial");
        AccessibilityNodeInfo minimum = waitControl(MINIMUM, SET_PROGRESS);
        AccessibilityNodeInfo maximum = waitControl(MAXIMUM, SET_PROGRESS);
        float initialMinimum = minimum.getRangeInfo().getCurrent();
        float initialMaximum = maximum.getRangeInfo().getCurrent();
        record("filters_initial", "minimum", nodeRecord(minimum).toString(), "maximum", nodeRecord(maximum).toString());
        require(minimum.getClassName().toString().contains("SeekBar"), "Minimum is a native adjustable SeekBar");
        require(maximum.getClassName().toString().contains("SeekBar"), "Maximum is a native adjustable SeekBar");
        checkModal("filters");

        // First widen the maximum if needed, keeping a nonzero range for the minimum check.
        require(maximum.getRangeInfo().getMax() == MAXIMUM_LENGTH_KM, "Maximum length range ends at 50 kilometers");
        if (initialMaximum < 2) setProgress(MAXIMUM, MAXIMUM_LENGTH_KM);
        minimum = waitControl(MINIMUM, SET_PROGRESS);
        float changedMinimum = initialMinimum == 0 ? 1 : 0;
        require(changedMinimum <= minimum.getRangeInfo().getMax(), "Minimum test value is allowed");
        setProgress(MINIMUM, changedMinimum);
        maximum = waitControl(MAXIMUM, SET_PROGRESS);
        float changedMaximum = maximum.getRangeInfo().getCurrent() == MAXIMUM_LENGTH_KM ? MAXIMUM_LENGTH_KM - 1 : MAXIMUM_LENGTH_KM;
        require(changedMaximum >= maximum.getRangeInfo().getMin(), "Maximum test value is allowed");
        setProgress(MAXIMUM, changedMaximum);
        click(findOrScroll("Cancel", true));
        await(() -> find(appRoot(), "Filters", AccessibilityNodeInfo.ACTION_CLICK) != null, "Explore after filter cancel");
        awaitNaturalFocusReturn("filter_cancel", cancelInvoker);
        AccessibilityNodeInfo backInvoker = findOrScroll("Filters", true);
        openWithTalkBack(backInvoker, "filters_reopened");
        require(waitControl(MINIMUM, SET_PROGRESS).getRangeInfo().getCurrent() == initialMinimum, "Cancel preserves applied minimum");
        require(waitControl(MAXIMUM, SET_PROGRESS).getRangeInfo().getCurrent() == initialMaximum, "Cancel preserves applied maximum");
        nativeBack();
        await(() -> find(appRoot(), "Filters", AccessibilityNodeInfo.ACTION_CLICK) != null, "native Back dismisses filter sheet");
        awaitNaturalFocusReturn("filter_back", backInvoker);
        record("filters_draft_cancel", "outcome", "passed");
    }

    private void checkSaveSheet() throws Exception {
        // The documented fixture precondition keeps this known trail in Explore's applied query.
        AccessibilityNodeInfo trail = findOrScroll(TRAIL_NAME, true);
        click(trail);
        await(() -> isTrailDetail(appRoot()), "known trail detail");
        AccessibilityNodeInfo saveInvoker = openSaveSheet();
        checkModal("save");
        Map<String, Boolean> original = memberships();
        record("save_original_membership", "checked", original.toString());
        String collection = "Weekend adventures";
        click(waitControl(collection, AccessibilityNodeInfo.ACTION_CLICK));
        await(() -> checkbox(collection).isChecked() != original.get(collection), "draft checkbox toggled");
        focus(checkbox(collection));
        record("save_draft_membership", "checked", memberships().toString());
        nativeBack();
        await(() -> isTrailDetail(appRoot()), "native Back dismisses save sheet to detail");
        require(find(appRoot(), "Save to a list", 0) == null, "Save modal is gone after Back");
        awaitNaturalFocusReturn("save_back", saveInvoker);
        AccessibilityNodeInfo secondSaveInvoker = openSaveSheet();
        require(memberships().equals(original), "Back discards draft and preserves current membership");
        nativeBack();
        await(() -> isTrailDetail(appRoot()), "detail after second save dismissal");
        awaitNaturalFocusReturn("save_back_reopened", secondSaveInvoker);
        click(findOrScroll("Saved", true));
        await(() -> isSelectedTab(appRoot(), "Saved"), "Saved tab selected after navigation");
        String previousDestination = null;
        for (int depth = 0; depth <= 4; depth++) {
            final String previous = previousDestination;
            await(() -> {
                String current = savedDestination(appRoot());
                return current != null && !current.equals(previous);
            }, "recognized Saved destination after " + (previous == null ? "tab selection" : "Back from " + previous));
            automation.waitForIdle(500, 5_000);
            String destination = savedDestination(appRoot());
            require(destination != null && !destination.equals(previous),
                    "Saved destination remains recognized after navigation events settle");
            record("saved_navigation_settled", "destination", destination, "backCount", Integer.toString(depth));
            if (destination.equals("root")) break;
            if (depth == 4) throw new AssertionError("Saved retained-route depth exceeds acceptance bound");
            // Only a confirmed, settled Saved detail/collection can receive Back. An
            // unknown transition, missing app window, or Saved root must never receive it.
            previousDestination = destination;
            nativeBack();
        }
        require("root".equals(savedDestination(appRoot())), "Saved root destination reached");
        record("saved_destination", "nodes", treeRecord(appRoot()).toString());
    }

    private boolean isSelectedTab(AccessibilityNodeInfo root, String label) {
        if (root == null) return false;
        if (root.isVisibleToUser() && ownLabel(root, label)) {
            AccessibilityNodeInfo node = root;
            for (int depth = 0; node != null && depth < 5; depth++, node = node.getParent()) {
                if (node.isSelected()) return true;
            }
        }
        for (int i = 0; i < root.getChildCount(); i++) {
            if (isSelectedTab(root.getChild(i), label)) return true;
        }
        return false;
    }

    private String savedDestination(AccessibilityNodeInfo root) {
        if (!isSelectedTab(root, "Saved") || isTrailModal(root)) return null;
        if (find(root, "Lists", 0) != null && find(root, "All trails", 0) != null &&
                (isSelectedTab(root, "Lists") || isSelectedTab(root, "All trails"))) return "root";
        if (find(root, "Back to saved", AccessibilityNodeInfo.ACTION_CLICK) != null) return "collection";
        if (isTrailDetail(root)) return "detail";
        return null;
    }

    private void openExplore() throws Exception {
        for (int i = 0; i < 5; i++) {
            // An app window can exist while Compose still shows bootstrap/splash. Never use
            // Back to discover that state: it can close the Activity before Trails is ready.
            awaitAppReady("Explore navigation");
            if (find(appRoot(), "Filters", AccessibilityNodeInfo.ACTION_CLICK) != null) return;
            if (isTrailModal(appRoot())) {
                nativeBack();
                continue;
            }
            // The selected Explore tab has no click action on its restored child route.
            // Pop only a recognized detail/collection, before looking for a tab click.
            if (isTrailDetail(appRoot()) ||
                    find(appRoot(), "Back to saved", AccessibilityNodeInfo.ACTION_CLICK) != null) {
                nativeBack();
                continue;
            }
            AccessibilityNodeInfo explore = find(appRoot(), "Explore", AccessibilityNodeInfo.ACTION_CLICK);
            if (explore != null) {
                click(explore);
            } else if (hasAppTabs(appRoot())) {
                // Explore is already selected; Filters can be outside the current viewport.
                findOrScroll("Filters", true);
                return;
            } else throw new AssertionError("Trails navigation is unavailable; refusing Back from an unknown screen");
            SystemClock.sleep(250);
        }
        findOrScroll("Filters", true);
    }

    private void awaitAppReady(String phase) throws Exception {
        awaitAppReady(phase, WAIT_MS, 100);
    }

    private void awaitAppReady(String phase, long timeoutMs, long pollMs) throws Exception {
        await(() -> isAppReady(appRoot()),
                "recognizable Trails content after " + phase + " (no Back while starting)", timeoutMs, pollMs);
        record("app_ready", "phase", phase);
    }

    private void awaitInitialAppReady() {
        long deadline = launchStartedAt + STARTUP_WAIT_MS;
        long nextDiagnosticAt = launchStartedAt + 10_000;
        while (SystemClock.uptimeMillis() < deadline) {
            AccessibilityNodeInfo root = appRoot();
            if (SystemClock.uptimeMillis() >= nextDiagnosticAt) {
                root = startupDiagnostic("startup_" + ((nextDiagnosticAt - launchStartedAt) / 1_000) + "s", root);
                nextDiagnosticAt += 10_000;
            }
            if (isAppReady(root)) {
                record("app_ready", "phase", "initial startup");
                return;
            }
            SystemClock.sleep(Math.min(250, Math.max(0, deadline - SystemClock.uptimeMillis())));
        }
        // The final diagnostic does not start another wait or extend the readiness budget.
        startupDiagnostic("startup_30s_deadline", appRoot());
        throw new AssertionError("Timed out: recognizable Trails content after initial startup (no Back while starting)");
    }

    private boolean isAppReady(AccessibilityNodeInfo root) {
        observeGraphBackedUi(root);
        if (root == null) return false;
        if (find(root, "Explore sample trails", AccessibilityNodeInfo.ACTION_CLICK) != null) {
            throw new AssertionError("An active sample account is required; startup reached Welcome");
        }
        if (hasBootstrapFailure(root)) {
            throw new AssertionError("Bootstrap failed; navigation actions were not attempted");
        }
        return find(root, "Filters", AccessibilityNodeInfo.ACTION_CLICK) != null || isTrailModal(root) || hasAppTabs(root);
    }

    private boolean hasAppTabs(AccessibilityNodeInfo root) {
        // Native selected tabs expose focus/selection without a redundant click action.
        return find(root, "Explore", 0) != null && find(root, "Saved", 0) != null &&
                (find(root, "Explore", AccessibilityNodeInfo.ACTION_CLICK) != null ||
                        find(root, "Saved", AccessibilityNodeInfo.ACTION_CLICK) != null);
    }

    private boolean hasBootstrapFailure(AccessibilityNodeInfo root) {
        return find(root, "We couldn’t open your saved trails. Try again to restore this account.", 0) != null ||
                find(root, "We couldn’t read the account saved on this device. Please try again.", 0) != null;
    }

    private void observeGraphBackedUi(AccessibilityNodeInfo root) {
        if (graphBackedUiObserved || root == null) return;
        graphBackedUiObserved = find(root, "Restoring your trails…", 0) != null || hasBootstrapFailure(root) ||
                find(root, "Explore sample trails", AccessibilityNodeInfo.ACTION_CLICK) != null ||
                find(root, "Filters", AccessibilityNodeInfo.ACTION_CLICK) != null || isTrailModal(root) || hasAppTabs(root);
    }

    private AccessibilityNodeInfo startupDiagnostic(String checkpoint, AccessibilityNodeInfo cachedRoot) {
        try {
            String cached = treeRecord(cachedRoot).toString();
            recordGraphSnapshot(checkpoint, cachedRoot);
            // This clears only this UiAutomation connection's node cache. It neither
            // suppresses TalkBack nor changes the application's state or accessibility focus.
            boolean cleared = automation.clearCache();
            AccessibilityNodeInfo freshRoot = appRoot();
            String fresh = treeRecord(freshRoot).toString();
            record("startup_cache_probe", "checkpoint", checkpoint,
                    "sinceLaunchMs", Long.toString(SystemClock.uptimeMillis() - launchStartedAt),
                    "clearCacheReturned", Boolean.toString(cleared),
                    "treeChanged", Boolean.toString(!cached.equals(fresh)),
                    "cachedTree", cached, "freshTree", fresh);
            if (!graphBackedUiObserved) recordGraphSnapshot(checkpoint + "_after_cache_clear", freshRoot);
            return freshRoot;
        } catch (Throwable failure) {
            record("startup_diagnostic_error", "checkpoint", checkpoint, "error", failure.toString());
            return cachedRoot;
        }
    }

    private void recordGraphSnapshot(String checkpoint, AccessibilityNodeInfo root) {
        observeGraphBackedUi(root);
        if (!graphBackedUiObserved) {
            record("app_graph_snapshot", "checkpoint", checkpoint,
                    "inspection", "deferred_until_graph_backed_content; App.runtime is lazy");
            return;
        }
        try {
            // The runtime initializes these services before rendering the observed UI.
            // Read only their current public state; never create a graph or invoke start/retry.
            App app = (App) getTargetContext().getApplicationContext();
            java.util.Map<String, String> snapshot = app.getRuntime().diagnosticSnapshot();
            record("app_graph_snapshot", "checkpoint", checkpoint,
                    "sinceLaunchMs", Long.toString(SystemClock.uptimeMillis() - launchStartedAt),
                    "bootstrapRouteClass", snapshot.get("bootstrapRouteClass"),
                    "backendConfigSync", snapshot.get("backendConfigSync"),
                    "userClass", snapshot.get("userClass"),
                    "backendConfig", snapshot.get("backendConfig"));
        } catch (Throwable failure) {
            record("app_graph_snapshot", "checkpoint", checkpoint, "inspectionError", failure.toString());
        }
    }

    private boolean isTrailModal(AccessibilityNodeInfo root) {
        return (find(root, "Filters", 0) != null &&
                find(root, "Close filters", AccessibilityNodeInfo.ACTION_CLICK) != null) ||
                find(root, "Save to a list", 0) != null || find(root, "Remove saved trail?", 0) != null;
    }

    private boolean isTrailDetail(AccessibilityNodeInfo root) {
        // Detail's bottom action remains visible when its heading has scrolled offscreen.
        return !isTrailModal(root) &&
                (find(root, "Save trail", AccessibilityNodeInfo.ACTION_CLICK) != null ||
                        find(root, "Edit saved collections", AccessibilityNodeInfo.ACTION_CLICK) != null);
    }

    private AccessibilityNodeInfo openSaveSheet() throws Exception {
        AccessibilityNodeInfo save = find(appRoot(), "Edit saved collections for " + TRAIL_NAME, AccessibilityNodeInfo.ACTION_CLICK);
        if (save == null) save = find(appRoot(), "Save " + TRAIL_NAME, AccessibilityNodeInfo.ACTION_CLICK);
        if (save == null) save = find(appRoot(), "Edit saved collections", AccessibilityNodeInfo.ACTION_CLICK);
        if (save == null) save = find(appRoot(), "Save trail", AccessibilityNodeInfo.ACTION_CLICK);
        if (save == null) save = findOrScroll("Save trail", true);
        openWithTalkBack(save, "save_sheet");
        waitControl("Weekend adventures", AccessibilityNodeInfo.ACTION_CLICK);
        return save;
    }

    private void resolveInputFilterInjection() throws Exception {
        try {
            // Android 15's hidden @TestApi is designed to test the accessibility filter.
            // The ordinary public injectInputEvent method deliberately bypasses that filter.
            injectInputFilter = UiAutomation.class.getMethod("injectInputEventToInputFilter", InputEvent.class);
            record("talkback_gesture_entrypoint", "method", injectInputFilter.toString(),
                    "available", "true", "scope", "androidTest only; accessibility input filter");
        } catch (ReflectiveOperationException failure) {
            record("talkback_gesture_entrypoint", "available", "false", "error", failure.toString());
            throw new AssertionError("Accessibility input-filter test API unavailable. If hidden-API enforcement blocks it, " +
                    "run this instrumentation process with --no-hidden-api-checks. No direct-focus fallback is permitted.", failure);
        }
    }

    private void openWithTalkBack(AccessibilityNodeInfo invoker, String phase) throws Exception {
        long startedAt = SystemClock.uptimeMillis();
        long deadline = startedAt + OPENER_GESTURE_WAIT_MS;
        AccessibilityNodeInfo root = appRoot();
        require(root != null, phase + " has an app window before TalkBack navigation");
        int windowId = root.getWindowId();
        await(() -> {
            AccessibilityNodeInfo initial = automation.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY);
            return initial != null && initial.getWindowId() == windowId;
        }, phase + " initial TalkBack focus before the first gesture",
                Math.min(WAIT_MS, remainingOpenerTime(deadline)), 100);
        awaitGestureSettlement(phase + "_initial", 0, deadline);
        Rect windowBounds = new Rect();
        root.getBoundsInScreen(windowBounds);
        require(windowBounds.width() > 0 && windowBounds.height() > 0, "TalkBack gesture bounds are available");
        AccessibilityNodeInfo focused = automation.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY);
        require(focused != null && focused.getWindowId() == windowId,
                "Initial TalkBack focus remains in the app window after event quiet");
        Rect invokerBounds = new Rect();
        Rect focusedBounds = new Rect();
        require(invoker.refresh() && invoker.isVisibleToUser(), phase + " actual invoker remains visible");
        invoker.getBoundsInScreen(invokerBounds);
        if (focused != null) focused.getBoundsInScreen(focusedBounds);
        // Root-tab navigation can leave focus below the invoking control. Select previous
        // or next once from visible geometry, then keep that direction for the bounded walk.
        boolean forward = focused == null || invokerBounds.centerY() >= focusedBounds.centerY();
        record("talkback_opener_start", "opener", phase, "direction", forward ? "right_next" : "left_previous",
                "expectedInvoker", nodeRecord(invoker).toString(), "focusedNode", nodeRecord(focused).toString(),
                "maximumGestures", Integer.toString(MAX_OPENER_GESTURES), "deadlineMs", Long.toString(OPENER_GESTURE_WAIT_MS));
        int gestures = 0;
        while (!withinInvokingControl(focused, invoker)) {
            long remaining = deadline - SystemClock.uptimeMillis();
            if (gestures >= MAX_OPENER_GESTURES || remaining <= 0) {
                throw new AssertionError("TalkBack gesture traversal did not reach actual invoker within bounds: " + phase);
            }
            AccessibilityNodeInfo before = focused;
            final boolean direction = forward;
            record("talkback_gesture_attempt", "opener", phase, "gesture", Integer.toString(gestures + 1),
                    "direction", forward ? "right_next" : "left_previous",
                    "entrypoint", "UiAutomation.injectInputEventToInputFilter",
                    "windowBounds", windowBounds.toShortString(), "before", nodeRecord(before).toString());
            AccessibilityEvent event = automation.executeAndWaitForEvent(
                    () -> injectTalkBackSwipe(windowBounds, direction),
                    e -> e.getEventType() == AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED &&
                            targetPackage.contentEquals(e.getPackageName() == null ? "" : e.getPackageName()) &&
                            e.getEventTime() >= lastInjectedSwipeStartTime &&
                            e.getWindowId() == windowId && e.getSource() != null && !e.getSource().equals(before),
                    Math.min(3_000, remaining));
            try {
                AccessibilityNodeInfo eventFocused = event.getSource();
                awaitGestureSettlement(phase + "_gesture_" + (gestures + 1), lastInjectedSwipeStartTime, deadline);
                focused = automation.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY);
                record("talkback_gesture_focus", "opener", phase, "gesture", Integer.toString(gestures + 1),
                        "direction", forward ? "right_next" : "left_previous",
                        "before", nodeRecord(before).toString(), "after", nodeRecord(focused).toString(),
                        "eventStillFocused", Boolean.toString(focused != null && focused.equals(eventFocused)),
                        "event", eventRecord(event).toString());
                require(focused != null && focused.getWindowId() == windowId && !focused.equals(before),
                        "TalkBack gesture changes actual accessibility focus inside the app window");
                require(focused.equals(eventFocused), "Gesture focus remains on its observed target after touch interaction ends");
                gestures++;
            } finally { event.recycle(); }
        }
        require(SystemClock.uptimeMillis() <= deadline, "TalkBack reaches invoker inside total gesture deadline");
        require(withinInvokingControl(automation.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY), invoker),
                "Actual sheet invoker owns accessibility focus before TalkBack activation");
        record("talkback_opener_focused", "opener", phase, "gestures", Integer.toString(gestures),
                "durationMs", Long.toString(SystemClock.uptimeMillis() - startedAt),
                "node", nodeRecord(automation.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY)).toString());
        record("talkback_opener_activation_attempt", "opener", phase,
                "action", "double_tap_via_accessibility_input_filter", "windowBounds", windowBounds.toShortString());
        AccessibilityEvent opened = automation.executeAndWaitForEvent(
                () -> injectTalkBackDoubleTap(windowBounds),
                e -> e.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
                        targetPackage.contentEquals(e.getPackageName() == null ? "" : e.getPackageName()),
                Math.min(WAIT_MS, remainingOpenerTime(deadline)));
        try {
            record("talkback_opener_activated", "opener", phase,
                    "action", "double_tap_via_accessibility_input_filter", "event", eventRecord(opened).toString());
        } finally { opened.recycle(); }
        await(() -> isTrailModal(appRoot()), "expected Trails modal after TalkBack double-tap",
                Math.min(WAIT_MS, remainingOpenerTime(deadline)), 100);
    }

    private long remainingOpenerTime(long deadline) {
        long remaining = deadline - SystemClock.uptimeMillis();
        if (remaining <= 0) throw new AssertionError("TalkBack opener exceeded its 30-second total deadline");
        return remaining;
    }

    private void awaitGestureSettlement(String phase, long swipeStartedAt, long deadline) throws Exception {
        long startedAt = SystemClock.uptimeMillis();
        boolean interactionEnded = false;
        boolean idle = false;
        try {
            await(() -> swipeStartedAt > 0
                            ? lastTouchInteractionEndTime >= swipeStartedAt
                            : lastTouchInteractionEndTime >= lastTouchInteractionStartTime,
                    phase + " touch interaction ends before another gesture",
                    Math.min(5_000, remainingOpenerTime(deadline)), 50);
            interactionEnded = true;
            automation.waitForIdle(500, Math.min(5_000, remainingOpenerTime(deadline)));
            idle = true;
            remainingOpenerTime(deadline);
        } finally {
            record("talkback_gesture_settled", "step", phase,
                    "swipeStartedAt", Long.toString(swipeStartedAt),
                    "touchInteractionStartAt", Long.toString(lastTouchInteractionStartTime),
                    "touchInteractionEndAt", Long.toString(lastTouchInteractionEndTime),
                    "interactionEnded", Boolean.toString(interactionEnded),
                    "eventIdle", Boolean.toString(idle), "quietPeriodMs", "500",
                    "durationMs", Long.toString(SystemClock.uptimeMillis() - startedAt),
                    "focusedNode", nodeRecord(automation.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY)).toString());
        }
    }

    private void injectTalkBackSwipe(Rect bounds, boolean forward) {
        long downTime = SystemClock.uptimeMillis();
        lastInjectedSwipeStartTime = downTime;
        float start = bounds.left + bounds.width() * (forward ? .25f : .75f);
        float end = bounds.left + bounds.width() * (forward ? .75f : .25f);
        float y = bounds.top + bounds.height() * .5f;
        injectFilteredMotion(downTime, MotionEvent.ACTION_DOWN, start, y);
        for (int sample = 1; sample <= 8; sample++) {
            SystemClock.sleep(20);
            injectFilteredMotion(downTime, MotionEvent.ACTION_MOVE, start + (end - start) * sample / 8, y);
        }
        injectFilteredMotion(downTime, MotionEvent.ACTION_UP, end, y);
    }

    private void injectTalkBackDoubleTap(Rect bounds) {
        float x = bounds.exactCenterX();
        float y = bounds.exactCenterY();
        for (int tap = 0; tap < 2; tap++) {
            long downTime = SystemClock.uptimeMillis();
            injectFilteredMotion(downTime, MotionEvent.ACTION_DOWN, x, y);
            SystemClock.sleep(40);
            injectFilteredMotion(downTime, MotionEvent.ACTION_UP, x, y);
            if (tap == 0) SystemClock.sleep(Math.min(100, ViewConfiguration.getDoubleTapTimeout() / 3));
        }
    }

    private void injectFilteredMotion(long downTime, int action, float x, float y) {
        MotionEvent.PointerProperties pointer = new MotionEvent.PointerProperties();
        pointer.id = 0;
        pointer.toolType = MotionEvent.TOOL_TYPE_FINGER;
        MotionEvent.PointerCoords position = new MotionEvent.PointerCoords();
        position.x = x;
        position.y = y;
        position.pressure = action == MotionEvent.ACTION_UP ? 0f : 1f;
        position.size = 1f;
        MotionEvent event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), action, 1,
                new MotionEvent.PointerProperties[]{pointer}, new MotionEvent.PointerCoords[]{position},
                0, 0, 1f, 1f, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0);
        try {
            injectInputFilter.invoke(automation, event);
        } catch (InvocationTargetException failure) {
            throw new AssertionError("Accessibility input-filter gesture injection failed", failure.getCause());
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("Accessibility input-filter test API could not be invoked", failure);
        } finally { event.recycle(); }
    }

    private Map<String, Boolean> memberships() throws Exception {
        Map<String, Boolean> values = new LinkedHashMap<>();
        for (String label : new String[]{"Weekend adventures", "Favorites"}) values.put(label, checkbox(label).isChecked());
        return values;
    }

    private AccessibilityNodeInfo checkbox(String label) throws Exception {
        AccessibilityNodeInfo node = waitControl(label, AccessibilityNodeInfo.ACTION_CLICK);
        require(node.isCheckable(), label + " exposes its checked state");
        return node;
    }

    private void setProgress(String label, float value) throws Exception {
        AccessibilityNodeInfo node = waitControl(label, SET_PROGRESS);
        focus(node);
        Bundle args = new Bundle();
        args.putFloat(AccessibilityNodeInfo.ACTION_ARGUMENT_PROGRESS_VALUE, value);
        require(node.performAction(SET_PROGRESS, args), label + " accepts native setProgress");
        await(() -> {
            AccessibilityNodeInfo current = find(appRoot(), label, SET_PROGRESS);
            return current != null && current.getRangeInfo() != null && current.getRangeInfo().getCurrent() == value;
        }, label + " range value changed");
        AccessibilityNodeInfo changed = waitControl(label, SET_PROGRESS);
        String expected = label.equals(MAXIMUM) && value == MAXIMUM_LENGTH_KM ? "No maximum length" : ((int) value) + " kilometers";
        require(expected.contentEquals(changed.getStateDescription() == null ? "" : changed.getStateDescription()), label + " announces updated length state");
        record("set_progress", "node", nodeRecord(changed).toString());
    }

    private void checkModal(String phase) throws Exception {
        long idleStartedAt = SystemClock.uptimeMillis();
        boolean settled = false;
        try {
            // Opening the native sheet emits window/content events that TalkBack also
            // handles. Let that initial event burst settle before requesting control focus.
            automation.waitForIdle(500, 5_000);
            settled = true;
        } finally {
            record("modal_event_idle", "modal", phase,
                    "quietPeriodMs", "500", "deadlineMs", "5000",
                    "durationMs", Long.toString(SystemClock.uptimeMillis() - idleStartedAt),
                    "settled", Boolean.toString(settled),
                    "focusedNode", nodeRecord(automation.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY)).toString());
        }
        AccessibilityNodeInfo root = appRoot();
        require(root != null, phase + " has active app modal window");
        int windowId = root.getWindowId();
        await(() -> {
            AccessibilityNodeInfo focused = automation.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY);
            return focused != null && focused.getWindowId() == windowId;
        }, phase + " native accessibility focus enters modal before manual traversal");
        AccessibilityNodeInfo initialFocus = automation.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY);
        require(initialFocus != null && initialFocus.getWindowId() == windowId,
                phase + " has native focus in modal before manual traversal");
        record("modal_initial_focus", "modal", phase, "node", nodeRecord(initialFocus).toString());
        require(find(root, "Search trails", 0) == null && find(root, "Filters", AccessibilityNodeInfo.ACTION_CLICK) == null &&
                find(root, "Explore", AccessibilityNodeInfo.ACTION_CLICK) == null && find(root, "Saved", AccessibilityNodeInfo.ACTION_CLICK) == null,
                phase + " active accessibility tree excludes underlying Explore/navigation controls");
        List<AccessibilityNodeInfo> controls = new ArrayList<>();
        collectControls(root, controls);
        require(!controls.isEmpty(), phase + " exposes actionable modal controls");
        for (AccessibilityNodeInfo control : controls) {
            if (control.getWindowId() != windowId || !control.isEnabled() || !control.isVisibleToUser()) continue;
            focus(control);
            AccessibilityNodeInfo focused = automation.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY);
            require(focused != null && focused.getWindowId() == windowId, phase + " focus stays in modal window");
        }
        record("modal_focus_isolation", "phase", phase, "controls", Integer.toString(controls.size()));
    }

    private void focus(AccessibilityNodeInfo node) throws Exception {
        require(node.refresh(), "Native focus target remains available");
        // Nodes collected for traversal are snapshots. A previously focused handle can
        // retain focused=true after another control gains focus; consult live ownership.
        AccessibilityNodeInfo currentFocus = automation.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY);
        if (currentFocus == null || !currentFocus.equals(node)) {
            AccessibilityEvent event = automation.executeAndWaitForEvent(
                    () -> require(node.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS), "Native accessibility focus accepted"),
                    e -> e.getEventType() == AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED && e.getSource() != null && e.getSource().equals(node),
                    WAIT_MS);
            record("focus_event", "event", eventRecord(event).toString());
            event.recycle();
        }
        AccessibilityNodeInfo focused = automation.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY);
        require(focused != null && focused.equals(node), "Requested native control owns accessibility focus");
        record("focused_control", "node", nodeRecord(focused).toString());
    }

    private void click(AccessibilityNodeInfo node) throws Exception {
        focus(node);
        record("click", "node", nodeRecord(node).toString());
        require(node.performAction(AccessibilityNodeInfo.ACTION_CLICK), "Native accessibility click accepted");
    }

    private void awaitNaturalFocusReturn(String phase, AccessibilityNodeInfo invoker) throws Exception {
        long startedAt = SystemClock.uptimeMillis();
        try {
            // Observe only. Do not request focus, click, scroll, or navigate before this passes.
            await(() -> withinInvokingControl(automation.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY), invoker),
                    "natural accessibility focus return after " + phase);
            require(withinInvokingControl(automation.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY), invoker),
                    "Focus naturally returns to the actual invoking control after " + phase);
        } finally {
            AccessibilityNodeInfo focused = automation.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY);
            AccessibilityNodeInfo inputFocused = automation.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
            String expectedInvoker = nodeRecord(invoker).toString();
            boolean invokerRefreshed = invoker.refresh();
            record("natural_focus_return", "dismissal", phase,
                    "durationMs", Long.toString(SystemClock.uptimeMillis() - startedAt),
                    "matched", Boolean.toString(withinInvokingControl(focused, invoker)),
                    "expectedInvoker", expectedInvoker,
                    "focusedNode", nodeRecord(focused).toString(),
                    "focusedTree", treeRecord(focused).toString(),
                    "inputFocusedNode", nodeRecord(inputFocused).toString(),
                    "invokerRefreshed", Boolean.toString(invokerRefreshed),
                    "refreshedInvokerTree", treeRecord(invokerRefreshed ? invoker : null).toString());
        }
    }

    private boolean withinInvokingControl(AccessibilityNodeInfo focused, AccessibilityNodeInfo invoker) {
        if (focused == null || !focused.isVisibleToUser() || focused.getWindowId() != invoker.getWindowId()) return false;
        AccessibilityNodeInfo candidate = focused;
        for (int depth = 0; candidate != null && depth < 8; depth++, candidate = candidate.getParent()) {
            if (candidate.equals(invoker)) return true;
            // Compose may expose a non-actionable label/role child of the actual control.
            // Another actionable descendant, or any ancestor outside the invoker, is not it.
            if (hasAction(candidate, AccessibilityNodeInfo.ACTION_CLICK) || hasAction(candidate, SET_PROGRESS)) return false;
        }
        return false;
    }

    private void nativeBack() {
        require(automation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK), "Native global Back accepted");
        SystemClock.sleep(300);
    }

    private AccessibilityNodeInfo waitControl(String label, int action) throws Exception {
        await(() -> find(appRoot(), label, action) != null, "control " + label);
        return find(appRoot(), label, action);
    }

    private AccessibilityNodeInfo findOrScroll(String label, boolean clickable) throws Exception {
        int action = clickable ? AccessibilityNodeInfo.ACTION_CLICK : 0;
        for (int direction : new int[]{AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD, AccessibilityNodeInfo.ACTION_SCROLL_FORWARD}) {
            for (int attempt = 0; attempt < 9; attempt++) {
                AccessibilityNodeInfo found = find(appRoot(), label, action);
                if (found != null) return found;
                AccessibilityNodeInfo scroller = firstWithAction(appRoot(), direction);
                if (scroller == null || !scroller.performAction(direction)) break;
                SystemClock.sleep(250);
            }
        }
        return waitControl(label, action);
    }

    private AccessibilityNodeInfo appRoot() {
        AccessibilityNodeInfo root = automation.getRootInActiveWindow();
        return root != null && targetPackage.contentEquals(root.getPackageName() == null ? "" : root.getPackageName()) ? root : null;
    }

    private AccessibilityNodeInfo find(AccessibilityNodeInfo root, String label, int action) {
        if (root == null) return null;
        if (root.isVisibleToUser() && ownLabel(root, label)) {
            AccessibilityNodeInfo candidate = root;
            for (int i = 0; candidate != null && i < 5; i++, candidate = candidate.getParent()) {
                if (action == 0 || hasAction(candidate, action)) return candidate;
            }
        }
        for (int i = 0; i < root.getChildCount(); i++) {
            AccessibilityNodeInfo found = find(root.getChild(i), label, action);
            if (found != null) return found;
        }
        return null;
    }

    private boolean ownLabel(AccessibilityNodeInfo node, String label) {
        return label.contentEquals(node.getText() == null ? "" : node.getText()) ||
                label.contentEquals(node.getContentDescription() == null ? "" : node.getContentDescription());
    }

    private boolean hasAction(AccessibilityNodeInfo node, int action) {
        for (AccessibilityNodeInfo.AccessibilityAction candidate : node.getActionList()) if (candidate.getId() == action) return true;
        return false;
    }

    private AccessibilityNodeInfo firstWithAction(AccessibilityNodeInfo root, int action) {
        if (root == null) return null;
        if (root.isVisibleToUser() && hasAction(root, action)) return root;
        for (int i = 0; i < root.getChildCount(); i++) {
            AccessibilityNodeInfo found = firstWithAction(root.getChild(i), action);
            if (found != null) return found;
        }
        return null;
    }

    private void collectControls(AccessibilityNodeInfo root, List<AccessibilityNodeInfo> nodes) {
        if (root == null) return;
        if (root.isVisibleToUser() && (hasAction(root, AccessibilityNodeInfo.ACTION_CLICK) || hasAction(root, SET_PROGRESS))) nodes.add(root);
        for (int i = 0; i < root.getChildCount(); i++) collectControls(root.getChild(i), nodes);
    }

    private void requireTalkBack(String phase) throws Exception {
        AccessibilityManager manager = (AccessibilityManager) getTargetContext().getSystemService(Context.ACCESSIBILITY_SERVICE);
        require(manager.isEnabled() && manager.isTouchExplorationEnabled(), "Accessibility and touch exploration remain enabled at " + phase);
        boolean spokenTalkBack = false;
        for (AccessibilityServiceInfo info : manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_SPOKEN)) {
            if (info.getId().toLowerCase().contains("talkback")) spokenTalkBack = true;
        }
        require(spokenTalkBack, "Enabled spoken-feedback TalkBack service at " + phase);
        String dump = accessibilityDump();
        String boundLine = "";
        for (String line : dump.split("\\n")) if (line.contains("Bound services:")) boundLine += line.trim();
        require(boundLine.toLowerCase().contains("talkback"), "TalkBack remains bound at " + phase);
        record("talkback_preserved", "phase", phase, "boundServices", boundLine, "touchExploration", "true",
                "automationFlags", "FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES");
    }

    private String accessibilityDump() throws Exception {
        ExecutorService reader = Executors.newSingleThreadExecutor();
        try (ParcelFileDescriptor descriptor = automation.executeShellCommand("dumpsys accessibility")) {
            Future<String> future = reader.submit(() -> {
                try (ParcelFileDescriptor.AutoCloseInputStream input = new ParcelFileDescriptor.AutoCloseInputStream(descriptor)) {
                    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                    byte[] buffer = new byte[4096];
                    int count;
                    while ((count = input.read(buffer)) != -1) {
                        bytes.write(buffer, 0, count);
                        if (bytes.size() > 512_000) throw new IllegalStateException("Accessibility dump exceeded bound");
                    }
                    return bytes.toString("UTF-8");
                }
            });
            return future.get(4, TimeUnit.SECONDS);
        } finally { reader.shutdownNow(); }
    }

    private interface Condition { boolean satisfied() throws Exception; }
    private void await(Condition condition, String description) throws Exception {
        await(condition, description, WAIT_MS, 100);
    }

    private void await(Condition condition, String description, long timeoutMs, long pollMs) throws Exception {
        long deadline = SystemClock.uptimeMillis() + timeoutMs;
        do {
            if (condition.satisfied()) return;
            SystemClock.sleep(pollMs);
        } while (SystemClock.uptimeMillis() < deadline);
        throw new AssertionError("Timed out: " + description);
    }

    private void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
    }

    private JSONObject nodeRecord(AccessibilityNodeInfo node) {
        JSONObject json = new JSONObject();
        if (node == null) return json;
        try {
            json.put("text", String.valueOf(node.getText()));
            json.put("description", String.valueOf(node.getContentDescription()));
            json.put("state", String.valueOf(node.getStateDescription()));
            json.put("class", String.valueOf(node.getClassName()));
            json.put("window", node.getWindowId());
            json.put("checked", node.isChecked());
            json.put("checkable", node.isCheckable());
            json.put("selected", node.isSelected());
            json.put("focused", node.isAccessibilityFocused());
            json.put("inputFocused", node.isFocused());
            json.put("focusable", node.isFocusable());
            Rect bounds = new Rect();
            node.getBoundsInScreen(bounds);
            json.put("boundsInScreen", bounds.toShortString());
            JSONArray actions = new JSONArray();
            for (AccessibilityNodeInfo.AccessibilityAction action : node.getActionList()) actions.put(action.getId() + ":" + action.getLabel());
            json.put("actions", actions);
            if (node.getRangeInfo() != null) json.put("rangeCurrent", node.getRangeInfo().getCurrent());
        } catch (Exception failure) { throw new IllegalStateException(failure); }
        return json;
    }

    private JSONObject eventRecord(AccessibilityEvent event) {
        return eventRecord(event, true);
    }

    private JSONObject eventRecord(AccessibilityEvent event, boolean includeSource) {
        JSONObject json = new JSONObject();
        try {
            json.put("type", AccessibilityEvent.eventTypeToString(event.getEventType()));
            json.put("time", event.getEventTime());
            json.put("package", String.valueOf(event.getPackageName()));
            json.put("text", event.getText().toString());
            json.put("description", String.valueOf(event.getContentDescription()));
            // Source retrieval and action inspection perform accessibility IPC. During cold
            // startup record event metadata only, leaving restoration free of that extra work.
            if (includeSource) json.put("source", nodeRecord(event.getSource()));
            else json.put("sourceOmitted", "startup event summary");
        } catch (Exception failure) { throw new IllegalStateException(failure); }
        return json;
    }

    private JSONArray treeRecord(AccessibilityNodeInfo root) {
        JSONArray rows = new JSONArray();
        appendTree(root, rows);
        return rows;
    }

    private void appendTree(AccessibilityNodeInfo node, JSONArray rows) {
        if (node == null || rows.length() >= 100) return;
        rows.put(nodeRecord(node));
        for (int i = 0; i < node.getChildCount(); i++) appendTree(node.getChild(i), rows);
    }

    private void record(String phase, String... fields) {
        JSONObject json = new JSONObject();
        try {
            json.put("phase", phase);
            json.put("elapsedMs", SystemClock.uptimeMillis());
            for (int i = 0; i < fields.length; i += 2) json.put(fields[i], fields[i + 1]);
        } catch (Exception failure) { throw new IllegalStateException(failure); }
        Bundle status = new Bundle();
        status.putString("stream", "TRAILS_A11Y " + json + "\n");
        sendStatus(0, status);
    }
}
