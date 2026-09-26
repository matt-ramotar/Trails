package org.mobilenativefoundation.trails.data.session

import org.mobilenativefoundation.store6.core.StoreKey
import org.mobilenativefoundation.store6.core.StoreNamespace

internal data object CurrentUserKey : StoreKey {
    override val namespace = StoreNamespace("session")
    override fun canonicalId(): String = "current"
}
