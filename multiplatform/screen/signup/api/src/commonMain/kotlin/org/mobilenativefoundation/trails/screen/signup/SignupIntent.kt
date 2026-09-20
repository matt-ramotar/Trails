package org.mobilenativefoundation.trails.screen.signup

import com.slack.circuit.runtime.CircuitUiEvent

sealed interface SignupIntent : CircuitUiEvent {
    data object NextStep : SignupIntent
    data object PreviousStep: SignupIntent
    data class InputEmail(val value: String) : SignupIntent
    data class InputPassword(val value: String) : SignupIntent
    data object ContinueWithGoogle : SignupIntent
    data object ContinueWithApple : SignupIntent
    data object LogIn : SignupIntent
    data object NavigateToTermsOfUse : SignupIntent
    data object NavigateToPrivacyPolicy : SignupIntent
}
