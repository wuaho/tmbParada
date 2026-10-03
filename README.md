# TMB Parada Widget

Widget nativo de Android escrito en Kotlin y Jetpack Glance para mostrar los
próximos buses de una parada de TMB Barcelona.

## Características

- En reposo no consulta la red ni mantiene un temporizador periódico.
- `RECARGAR` inicia una ventana de tiempo real de 10 minutos.
- Durante esa ventana se ejecuta una consulta de TMB cada 30 segundos mediante
  trabajos puntuales de WorkManager.
- El último trabajo limpia el estado activo al terminar la ventana de 10 minutos.
- El estado, los últimos minutos y la hora de actualización se conservan en
  `SharedPreferences`, para que el widget siga siendo útil entre reinicios.
- La UI usa Glance y colores día/noche compatibles con Material You.

## API de TMB

1. Entra en [TMB Open Data](https://developer.tmb.cat/).
2. Crea una cuenta o inicia sesión.
3. Solicita una aplicación desde el área de credenciales y copia `app_id` y
   `app_key`.
4. Añade estas propiedades a `local.properties` (conserva cualquier `sdk.dir`
   que Android Studio ya haya generado):

   ```properties
   TMB_APP_ID=tu_app_id
   TMB_APP_KEY=tu_app_key
   TMB_STOP_ID=1001
   ```

   `TMB_STOP_ID` es el código numérico de la parada que quieres consultar.
5. No subas `local.properties` al control de versiones. Las credenciales se
   convierten en campos de `BuildConfig` durante la compilación y no se
   incluyen en el repositorio.

La petición usada por la aplicación es:

```text
GET https://api.tmb.cat/v1/ibus/stops/{stop_id}?app_id={app_id}&app_key={app_key}
```

## Compilar e instalar

Requisitos: Android Studio reciente, Android SDK 35 y JDK 17 o posterior.

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Abre la aplicación para comprobar la configuración y añade **TMB Parada**
desde el selector de widgets de la pantalla de inicio.

## Arquitectura del refresco

El callback del botón guarda `activeUntilEpochMillis` y encola un
`OneTimeWorkRequest`. El worker consulta TMB, actualiza el estado y encola el
siguiente trabajo usando `APPEND_OR_REPLACE`. Cuando queda menos de un
intervalo para el final, encola un último trabajo corto que vuelve al modo
reposo. El `updatePeriodMillis` del proveedor del widget es `0`, por lo que
Android no inicia actualizaciones periódicas en segundo plano.

## Personalización

- Parada por defecto: `TMB_STOP_ID` en `local.properties`.
- Intervalo de refresco: `REFRESH_INTERVAL_SECONDS` en
  `app/src/main/java/com/example/tmbparada/data/TmbConfig.kt`.
- Duración del modo directo: `LIVE_WINDOW_MILLIS` en el mismo archivo.
