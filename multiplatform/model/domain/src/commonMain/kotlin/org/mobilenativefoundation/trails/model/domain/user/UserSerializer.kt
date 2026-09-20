package org.mobilenativefoundation.trails.model.domain.user

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject

object UserSerializer : JsonContentPolymorphicSerializer<User>(User::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<User> {
        if (!element.jsonObject.containsKey("node")) return LoggedOutUser.serializer()
        val onboardingStatus = OnboardingStatus.fromJsonElement(element)
        return when (onboardingStatus) {
            OnboardingStatus.Complete -> ActiveUser.Composite.serializer()
            OnboardingStatus.Incomplete -> InactiveUser.Composite.serializer()
        }
    }
}