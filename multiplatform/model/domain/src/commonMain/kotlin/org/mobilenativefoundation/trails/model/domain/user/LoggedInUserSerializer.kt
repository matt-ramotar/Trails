package org.mobilenativefoundation.trails.model.domain.user

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement

object LoggedInUserSerializer : JsonContentPolymorphicSerializer<LoggedInUser>(LoggedInUser::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<LoggedInUser> {
        val onboardingStatus = OnboardingStatus.fromJsonElement(element)
        return when (onboardingStatus) {
            OnboardingStatus.Complete -> ActiveUser.Composite.serializer()
            OnboardingStatus.Incomplete -> InactiveUser.Composite.serializer()
        }
    }
}



