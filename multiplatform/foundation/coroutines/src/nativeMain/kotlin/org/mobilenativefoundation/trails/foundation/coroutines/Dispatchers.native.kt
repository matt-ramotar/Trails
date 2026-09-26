package org.mobilenativefoundation.trails.foundation.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

actual val Dispatchers.Io: CoroutineDispatcher
    get() = Dispatchers.Unconfined
