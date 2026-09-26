package org.mobilenativefoundation.trails.model.domain.user

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
enum class OnboardingStatus {
    @SerialName(INCOMPLETE)
    Incomplete,

    @SerialName(COMPLETE)
    Complete;

    companion object {
        internal const val INCOMPLETE = "INCOMPLETE"
        internal const val COMPLETE = "COMPLETE"

        fun fromJsonElement(element: JsonElement): OnboardingStatus {
            val onboardingStatus = element
                .jsonObject["node"]
                ?.jsonObject?.get("properties")
                ?.jsonObject?.get("onboardingStatus")
                ?.jsonPrimitive?.content

            return when (onboardingStatus) {
                Complete.name,
                COMPLETE -> Complete

                Incomplete.name,
                INCOMPLETE -> Incomplete

                else -> {
                    error("Unknown onboarding status: $onboardingStatus")
                }
            }
        }
    }
}


