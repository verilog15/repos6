package ch.rmy.android.http_shortcuts.activities.variables.editor.types

import androidx.compose.runtime.Stable

@Stable
data class IncrementTypeViewState(
    val value: String,
) : VariableTypeViewState
