package org.mobilenativefoundation.trails.app.bootstrap

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides

@BindingContainer
object BootstrapBindings {
    @Provides
    fun provideSplashStateReader(manager: SplashStateManager): org.mobilenativefoundation.trails.app.bootstrap.SplashStateReader = manager

    @Provides
    fun provideSplashStateWriter(manager: SplashStateManager): org.mobilenativefoundation.trails.app.bootstrap.SplashStateWriter = manager
}