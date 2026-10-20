package com.example.tmbparada

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import androidx.glance.appwidget.updateAll
import com.example.tmbparada.widget.TmbWidget
import kotlinx.coroutines.launch

class TmbApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }

    /** Widgets do not redraw on their own when the app language changes. */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        container.applicationScope.launch { TmbWidget().updateAll(this@TmbApplication) }
    }
}

val Context.appContainer: AppContainer
    get() = (applicationContext as TmbApplication).container
