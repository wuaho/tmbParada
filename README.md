# TMB Stop Widget

Native Android home-screen widget, written in Kotlin with Jetpack Glance, that shows
when the next TMB Barcelona buses reach a stop you choose.

## Features

- **One stop per widget.** Search the offline catalog of TMB stops by name, code, or
  line when you add the widget. Tap the stop name on the widget (or use the app) to
  change it later.
- **Arrivals grouped by line and destination**, with the next two buses per row and
  line badges in the TMB network colors (H blue, V green, D purple, others red). The
  number of rows adapts to the widget size.
- **Zero cost while idle.** The widget never polls in the background. Tapping ↻ starts
  a ten-minute live session that refreshes every 30 seconds and then stops.
- **Honest times.** Arrivals are stored as absolute timestamps, so the countdown is
  recomputed on every render, rounded down. Data older than five minutes is shown as
  approximate (`~3 min`) and data older than an hour is hidden.
- **Clear status.** The header shows whether the widget is live, how old its data is,
  or why the last update failed (no connection, missing or rejected API keys, …).
- Material You colors on the widget and the app, light and dark themes, and English,
  Spanish, and Catalan translations.

## TMB API credentials

1. Sign in at [TMB Open Data](https://developer.tmb.cat/) and create an application to
   obtain an `app_id` and an `app_key`.
2. Add them to `local.properties` (keep the `sdk.dir` line Android Studio generates):

   ```properties
   TMB_APP_ID=your_app_id
   TMB_APP_KEY=your_app_key
   ```

   Environment variables with the same names are used when the properties are absent,
   which is convenient for CI.
3. Never commit `local.properties`. The values are compiled into `BuildConfig`.

Live arrivals come from:

```text
GET https://api.tmb.cat/v1/ibus/stops/{stop_code}?app_id={app_id}&app_key={app_key}
```

## Build and install

Requirements: recent Android Studio, Android SDK 35, and JDK 17 or later.

```bash
./gradlew testDebugUnitTest      # unit tests
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Add **TMB Stop** from the launcher's widget picker, or open the app and tap
**Add widget**. The stop picker opens automatically.

## Architecture

```text
app/src/main/java/com/example/tmbparada/
├── domain/          Pure Kotlin: models, repository interfaces, refresh logic
│   ├── model/       BusStop, Arrival, ArrivalsSnapshot, WidgetState, RefreshError
│   ├── refresh/     LiveRefreshPolicy (state transitions), LiveRefreshController
│   └── BusLines     Line ordering and colors
├── data/
│   ├── arrivals/    OkHttp client, tolerant JSON parser, ArrivalsRepository impl
│   ├── catalog/     Room database over the packaged stop catalog (FTS4 search)
│   └── widgetstate/ DataStore persistence of WidgetState (JSON per widget)
├── widget/          Glance widget, refresh action, WorkManager scheduler/worker
│   └── ui/          WidgetPresenter (state → UI model, pure) and Glance composables
├── config/          Stop picker: Activity, ViewModel, Compose screen
├── ui/              Home screen and Material theme
├── AppContainer     Manual dependency injection, one instance per process
└── TmbApplication
```

Dependencies point inwards: `widget`, `config`, and `ui` depend on `domain`
interfaces, and `data` implements them. Everything in `domain`, the parser, the state
codec, and the widget presenter is plain Kotlin and covered by JVM unit tests.

### Data flow

1. The stop picker calls `LiveRefreshController.configure`, which stores the stop and
   starts a live session.
2. A live session stores `liveUntilMillis` and enqueues a one-time `LiveRefreshWorker`.
   Each worker run calls `runRefreshStep`: fetch arrivals, store the result, and chain
   the next run with `APPEND_OR_REPLACE` — or end the session once the window closes.
3. `TmbWidget` observes the widget's state in DataStore and renders it through
   `WidgetPresenter`, so every write updates the home screen. The provider sets
   `updatePeriodMillis="0"`: Android never wakes the app on its own.

### Stop catalog

`app/src/main/assets/tmb_transit.db` is a Room-compatible SQLite database with the
tables `bus_lines`, `stops`, `line_stops`, and the FTS4 index `stops_fts`
(accent-free). To ship a new catalog, regenerate the file with a higher
`PRAGMA user_version` and set the same number as the `version` of `TransitDatabase`;
Room then replaces the installed copy on upgrade. The entities must keep matching the
asset's schema, because Room validates its identity hash.

## Customization

- Live session length and refresh interval: `LiveRefreshPolicy` defaults.
- Staleness thresholds: constants in `WidgetPresenter`.
- Line colors: `BusLines`.
