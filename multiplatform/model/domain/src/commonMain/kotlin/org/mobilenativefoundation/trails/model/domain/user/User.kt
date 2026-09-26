package org.mobilenativefoundation.trails.model.domain.user


import kotlinx.serialization.Serializable


@Serializable(with = UserSerializer::class)
sealed interface User


