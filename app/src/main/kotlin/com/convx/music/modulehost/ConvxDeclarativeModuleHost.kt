package com.convx.music.modulehost

import com.convx.music.BuildConfig
import com.convx.modulehost.DeclarativeModuleHost
import com.convx.modulehost.DeclarativeModuleHostLifecycle
import com.convx.modulehost.DeclarativeModuleHostSnapshot
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConvxDeclarativeModuleHost @Inject constructor() {
    private val host = DeclarativeModuleHost(BuildConfig.VERSION_NAME)

    @Synchronized
    fun start() {
        if (host.snapshot().lifecycle == DeclarativeModuleHostLifecycle.NOT_STARTED) {
            host.start()
        }
    }

    fun snapshot(): DeclarativeModuleHostSnapshot = host.snapshot()
}

object DeclarativeModuleHostRoutes {
    const val SETTINGS = "settings/declarative-modules"
}
