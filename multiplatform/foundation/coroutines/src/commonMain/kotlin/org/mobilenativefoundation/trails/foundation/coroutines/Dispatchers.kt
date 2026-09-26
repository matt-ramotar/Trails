package org.mobilenativefoundation.trails.foundation.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

expect val Dispatchers.Io: CoroutineDispatcher
