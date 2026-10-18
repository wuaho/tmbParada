package com.example.tmbparada

import android.content.Context
import com.example.tmbparada.data.arrivals.TmbApiClient
import com.example.tmbparada.data.arrivals.TmbArrivalsRepository
import com.example.tmbparada.data.arrivals.TmbCredentials
import com.example.tmbparada.data.catalog.RoomStopCatalog
import com.example.tmbparada.data.catalog.TransitDatabase
import com.example.tmbparada.data.widgetstate.DataStoreWidgetStateStore
import com.example.tmbparada.data.widgetstate.widgetStateDataStore
import com.example.tmbparada.domain.ArrivalsRepository
import com.example.tmbparada.domain.Clock
import com.example.tmbparada.domain.StopCatalog
import com.example.tmbparada.domain.WidgetStateStore
import com.example.tmbparada.domain.refresh.LiveRefreshController
import com.example.tmbparada.domain.refresh.LiveRefreshPolicy
import com.example.tmbparada.widget.GlanceWidgetRenderer
import com.example.tmbparada.widget.InstalledWidgets
import com.example.tmbparada.widget.work.WorkManagerRefreshScheduler
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/** Manual dependency graph, created once per process by [TmbApplication]. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val clock: Clock = Clock.System

    val credentials = TmbCredentials(
        appId = BuildConfig.TMB_APP_ID,
        appKey = BuildConfig.TMB_APP_KEY,
    )

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    val arrivalsRepository: ArrivalsRepository by lazy {
        TmbArrivalsRepository(TmbApiClient(httpClient, credentials), credentials, clock)
    }

    val stopCatalog: StopCatalog by lazy {
        RoomStopCatalog(TransitDatabase.create(appContext).transitDao())
    }

    val widgetStateStore: WidgetStateStore by lazy {
        DataStoreWidgetStateStore(appContext.widgetStateDataStore)
    }

    val installedWidgets: InstalledWidgets by lazy { InstalledWidgets(appContext) }

    val liveRefresh: LiveRefreshController by lazy {
        LiveRefreshController(
            store = widgetStateStore,
            arrivals = arrivalsRepository,
            scheduler = WorkManagerRefreshScheduler(appContext),
            renderer = GlanceWidgetRenderer(appContext),
            policy = LiveRefreshPolicy(),
            clock = clock,
        )
    }
}
