# Trails

Trails is a Kotlin Multiplatform hiking sample built with Compose Multiplatform,
Circuit, Metro, Store6, and Atom. It demonstrates persisted trail browsing,
account-scoped saved lists, and durable offline saves against an in-process fake
backend.

Explore, For You, Navigate, Saved, and Activity have independent navigation
stacks. Recommendations and activity history are sample data. Navigate displays
a labelled schematic route; maps, location tracking, and recording are not
implemented.

## Run on Android

Prepare the pinned local libraries using [dependency setup](docs/dependency-setup.md),
then build and install:

```bash
./gradlew :apps:android:assembleDebug :apps:android:installDebug
```

Android is the runnable sample host. The shared modules also declare JVM, iOS,
and JavaScript targets. Resolving those targets does not establish application
or persistence support on each platform.

## Development

Run the architecture checks without Gradle:

```bash
python3 scripts/check_architecture.py
```

- [Architecture](docs/architecture.md): module responsibilities and state ownership.
- [Behavior](docs/behavior.md): navigation, queries, saves, and recovery.
- [Testing](docs/testing.md): local checks and Android acceptance.
- [Trail catalog](docs/trail-catalog.md): fixture semantics and route sources.
- [Bundled assets](multiplatform/foundation/designsystem/ASSETS.md) and
  [photography](multiplatform/ui/trail/TRAIL_PHOTOS.md): provenance and licenses.
- [Contributor instructions](AGENTS.md): package conventions and verification rules.

## License

```text
Copyright 2024 Mobile Native Foundation

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
