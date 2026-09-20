# Persistent shell device actions

All actions target only emulator-5554 through one `adb -s emulator-5554 shell -tt` session (local session12264). Commands and observed results are transcribed from task8 tool output; these entries do not invent per-action timestamps. Capture timestamps/hashes are independently recorded in android/actions.jsonl by roots-pull.py. Each successful capture uses a fresh numbered `/sdcard/<name>.xml` from uiautomator and PNG from screencap, then adb sync pull. A failed export is never reused.

- 20: readiness `getprop sys.boot_completed` →1; fresh `uiautomator dump /sdcard/roots-persistent-20.xml` → dumped; `screencap -p /sdcard/roots-persistent-20.png`; sync pulls. Drawer shows Offline unchecked.
- 21: `input tap 955 1060` toggled Offline; `input tap 985 290` closed drawer. Capture21: cached For You six rows without error.
- 22: `am force-stop org.mobilenativefoundation.trails.android`; `run-as org.mobilenativefoundation.trails.android tar -cf - databases shared_prefs > /data/local/tmp/roots-03-foryou-offline.tar`; `am start -n org.mobilenativefoundation.trails.android/.MainActivity`; capture22 shows selected For You and cached scroll/content. Database03 independently records OFFLINE.
- 23: tap Navigate at640,2630; capture shows last-opened Mist Trail and schematic/recording controls.
- 24: tap name pill470,285; capture shows Mist Trail detail with Navigate selected.
- 25: system Back (`input keyevent4`), tap Start recording640,2370; capture shows `Recording is coming soon` without a tick.
- 26: after capture25 retrieval and inspection, capture26 shows no toast. This proves expiry, not a precisely bounded five-second deadline; exact timing needs a separate measurement.
- 27: tap Explore180,2630 then intended Half Dome card500,1300 without transition delay; tap Navigate640,2630. Capture remained Mist Trail, so this sequence DOES NOT prove following a different Explore trail.
- 28: enabled three-button overlay then immediately swiped20,1400→800,1400; navigation transition had not settled and Android Back returned to launcher. Not a product failure or valid drawer capture.
- 29: subsequent intended drawer/action coordinates were issued against that launcher and opened Google Calendar welcome. No Calendar setup/data operation occurred. Capture retained as driving error; no Activity claim.
- 30: launched Trails, enabled three-button mode, then opened drawer before app readiness; uiautomator returned `ERROR: null root node returned by UiTestAutomationBridge`. PNG shows splash. No XML exists.
- 31: waited for actual readiness; new export shows Navigate. Three-button mode is now visibly active.
- 32: swipe20,1400→800,1400 after transition settled. Drawer captured Offline checked, Applied.
- 33: tap Offline955,1060; separately tap close985,290; tap Activity1090,2590. Captured actual online Activity46.9km/5trails/22h and Yesterday hero.
- 34: force-stop, run-as databases/shared_prefs archive04, am start. Capture34 restores Activity root. Archive04 contains seeded backend activity rows and computed month sum.
- 35: tap Mount Takao row580,2180 after readiness; capture actual matching detail.
- 36: force-stop, run-as shared_prefs to roots-activity-detail-checkpoint.tar, am start; capture36 restores Activity-selected Mount Takao detail. Checkpoint captured independently.
- 37: immediate Back then heart tap raced the transition; capture37 remains Activity. No save-sheet claim.
- 38: after transition, tap heart1148,2215, sleep1, fresh export; Mount Takao save sheet visible.
- 39: tap Weekend450,1950, sleep1, tap Save660,2350, sleep1. Export returned null root; PNG independently shows `Saved to Weekend adventures` toast. Screenshot-only evidence; later41 verifies saved-heart label.
- 40: after sleep5, open drawer; drag error-rate250,1880→1040,1880. Actual captured setting is0.80, NOT1.00 despite attempted-state filename. Offline false.
- 41: close drawer, sleep1, force-stop/start; fresh capture shows cached Activity with no failure or Try again. Does not prove Retry behavior.
- 42: reopen drawer, tap1040,1880. Actual error rate remains0.80; no deterministic100% failure claim.
- 43: dragged actual error-rate thumb840,1878→1050,1878; captured Applied, Offline false, error rate1.00.
- 44: close drawer, sleep1, force-stop, database/shared_prefs archive05, start, sleep3; export captured bootstrap `Restoring your trails…`, not the settled Activity result. Do not count this as a stable successful/failing feed assertion.
- 45: opened drawer after launch, dragged max error-rate thumb1020,1878→30,1878, toggled Offline. Captured Applied, error rate0.00 and Offline checked.
- 46: close drawer, sleep1; settled cached Activity content remains, no error/`Try again` appears. The prescribed Retry tap remains unexercised; no pass inferred from action absence.
- After46: force-stop, archive06 database/shared_prefs, start. This preserves Offline configuration and cached Activity plus the saved Mount Takao membership without database mutation.
- 47: opened drawer after archive06 restart, toggled Offline off. Captured Applied, Offline unchecked, error rate0.00, latency50–200ms, rate limit0. No reset was used.
- 48: closed drawer, sleep1, selected Explore, sleep1. Captured default Explore50 trails.
- 49: tapped visible Half Dome title200,1530, sleep1. Captured actual Half Dome detail with Explore selected.
- 50: selected Navigate640,2590, sleep1. Captured name pill now Half Dome (previously Mist Trail), proving last-opened propagation from Explore.
- 51: force-stop/start with Navigate selected, sleep4; first uiautomator export returned null root. Screenshot-only retained.
- 52: later fresh capture still showed `Restoring your trails…`; not a completed restart assertion. The intended pill tap300,285 occurred while restoring, so subsequent navigate-detail-checkpoint archive cannot be assumed to contain a pushed detail. Its actual decoded route must be checked.
- 53: after the later restart and readiness delay, the new pair shows Navigate selected, Half Dome pill and schematic controls. This closes root restoration (51/52 retain initial loading/export failures). Startup logcat preserved; no root-ready timeout was hidden.
- 54: after actual Navigate root readiness, tapped Half Dome pill; captured detail with Navigate selected.
- 55/56: force-stop, archived actual navigate-pushed-checkpoint, start.55 still shows bootstrap; later56 shows restored Half Dome detail with Navigate selected. Decoded checkpoint independently contains NAVIGATE and navigate→trail:half-dome.
- 57: selected For You, sleep2, tapped visible Mist Trail row540,680, sleep1. Capture confirms Mist Trail detail with For You selected. Force-stop, archive foryou-pushed-checkpoint, start for restoration check.
- 58: after startup readiness, For You-selected Mist Trail detail is restored. The separate archive pull remained stalled; this does not negate the captured UI restoration, but no unread archive is claimed as checkpoint evidence.
- Before59: system Back to For You, sleep2, swipe down to feed top, sleep1; restore original gestural navigation overlay; set system font_scale2.0 for all three roots' large-text verification.

## Controller completion on corrected APK

- After6e56873 build, install -r through isolatedADB5038 returned Success. pm path and device sha256sum matched locald94158f… (full hash in nav-fix-build-install.json).64/65 are retained without assigned source identity; they are not final corrected-build acceptance.
- Initial settings read font_scale2.0, accessibility_enabled0, gestural overlay active.66 captured Activity200%; helper tapped For You from fresh tree and67 captured it; helper tapped Navigate from fresh tree and68 captured it. All three showed full navigation labels in two rows. Activitymonth and ForYou campaign fit; Navigate name/action remained visible.
- Set font_scale1.0, read back1.0, captured69 default single-row navigation. roots-timed-toast.py tapped Start recording using69's measured bounds, captured70 with toast/no tick and71 after5.206s without toast; logs/recording-toast-timing.json records PNG capture intervals and later XML export times.
- Read final system settings and overlays, force-stopped Trails, archived databases/shared_prefs to /data/local/tmp/roots-07-final-controller.tar, relaunched the app, pulled archive through isolated5038 and inspected its backend defaults read-only. No database write/clear/wipe. Final seed4, default ONLINE/errorRate0; system font1/gestural/accessibility0 restored.
- Closed only the controller shell and isolatedADB5038 server after verification.
