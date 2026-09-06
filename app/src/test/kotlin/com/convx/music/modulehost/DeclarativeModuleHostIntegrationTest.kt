package com.convx.music.modulehost

import com.convx.modulehost.DeclarativeModuleHostLifecycle
import org.junit.Assert.assertEquals
import org.junit.Test

class DeclarativeModuleHostIntegrationTest {
    @Test
    fun `application host starts idempotently and exposes its settings route`() {
        val host = ConvxDeclarativeModuleHost()

        assertEquals(DeclarativeModuleHostLifecycle.NOT_STARTED, host.snapshot().lifecycle)
        host.start()
        host.start()

        assertEquals(DeclarativeModuleHostLifecycle.STARTED, host.snapshot().lifecycle)
        assertEquals("settings/declarative-modules", DeclarativeModuleHostRoutes.SETTINGS)
    }
}
