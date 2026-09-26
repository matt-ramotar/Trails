# Controller capture recovery

The original task8 and first follow-up agents were interrupted after status messages stopped. Existing tool session12264 could not be reused from the controller: write_stdin returned Unknown process id12264. New install and persistent-shell clients on shared port5037 stalled without output and were interrupted; no success was inferred. Serial-scoped reconnect returned reconnecting emulator-5554, but a new shell still stalled. A bounded ADB_TRACE=transport,sockets diagnostic stopped at adb_client.cpp:_adb_connect host:version; interrupted after preservation.

Started a separate server with ADB_LOCAL_TRANSPORT_MAX_PORT=5555 ADB_MDNS_AUTO_CONNECT='' adb -P5038 start-server. It returned daemon started successfully. Its devices list contained only emulator-5554. The shared server was not stopped and emulator-5556 was not targeted. The corrected APK installation then returned Performing Streamed Install / Success. A new persistent shell opened promptly. Font scale2.0, accessibility_enabled0 and gestural overlay active were read before final captures.

Capture helpers accept ROOTS_ADB_PORT (default5037); final controller captures use5038. The isolated server must be stopped after verification; this does not clear or stop app data.

Final verification succeeded using isolated5038: APK hash matched built d94158f5329c3cd3f653fb55bb2f3f3b7c7573ab628d034ed249dd8dd74e2138. Fresh captures66–71 and final settings/database report were completed. Exited controller persistent shell and adb -P5038 kill-server completed exit0; shared5037 server was not stopped.
