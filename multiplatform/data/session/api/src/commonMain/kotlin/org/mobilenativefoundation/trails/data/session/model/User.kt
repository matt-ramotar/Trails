package org.mobilenativefoundation.trails.data.session.model


import kotlinx.serialization.Serializable


@Serializable(with = UserSerializer::class)
sealed interface User


