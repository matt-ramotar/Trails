package org.mobilenativefoundation.trails.app.runtime.bootstrap

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides

@BindingContainer
internal object BootstrapBindings {
    @Provides
    fun provideSplashStateReader(manager: SplashStateManager): SplashStateReader = manager

    @Provides
    fun provideSplashStateWriter(manager: SplashStateManager): SplashStateWriter = manager
}
