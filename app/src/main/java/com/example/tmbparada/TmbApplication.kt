package com.example.tmbparada

import android.app.Application
import android.content.Context

class TmbApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}

val Context.appContainer: AppContainer
    get() = (applicationContext as TmbApplication).container
