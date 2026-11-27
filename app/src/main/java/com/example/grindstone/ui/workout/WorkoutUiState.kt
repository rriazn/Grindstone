package com.example.grindstone.ui.workout

import com.example.grindstone.data.Exercise
import java.time.LocalDate
import java.time.LocalTime

data class WorkoutUiState(
    val isLoading: Boolean = true,
    val exercises: List<Exercise> = emptyList(),
    val selectedExerciseId: String? = null,
    val repsInput: String = "",
    val weightInput: String = "",
    /** Null while the field is valid or still empty. */
    val repsError: InputError? = null,
    val weightError: InputError? = null,
    val canAddSet: Boolean = false,
    /** Newest day first; sets within a day newest first. */
    val days: List<DayGroup> = emptyList(),
)

data class DayGroup(val date: LocalDate, val sets: List<LoggedSet>)

data class LoggedSet(
    val id: String,
    val exerciseId: String,
    val reps: Int,
    val weightKg: Double,
    val time: LocalTime,
)

sealed interface WorkoutEvent {
    data class SelectExercise(val exerciseId: String) : WorkoutEvent
    data class RepsChanged(val text: String) : WorkoutEvent
    data class WeightChanged(val text: String) : WorkoutEvent
    data object AddSet : WorkoutEvent
    data class DeleteSet(val setId: String) : WorkoutEvent
}
