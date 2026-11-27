package com.example.grindstone.ui.workout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.grindstone.data.Exercise
import com.example.grindstone.data.ExerciseRepository
import com.example.grindstone.data.WorkoutRepository
import com.example.grindstone.data.WorkoutSet
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class WorkoutViewModel @Inject constructor(
    exerciseRepository: ExerciseRepository,
    private val workoutRepository: WorkoutRepository,
    private val clock: Clock,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private data class Form(val reps: String = "", val weight: String = "")

    private val form = MutableStateFlow(Form())

    val uiState: StateFlow<WorkoutUiState> = combine(
        exerciseRepository.observeExercises(),
        workoutRepository.observeSets(),
        savedStateHandle.getStateFlow<String?>(KEY_SELECTED_EXERCISE, null),
        form,
    ) { exercises, sets, selectedId, form ->
        buildState(exercises, sets, selectedId, form)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WorkoutUiState())

    fun onEvent(event: WorkoutEvent) {
        when (event) {
            is WorkoutEvent.SelectExercise -> savedStateHandle[KEY_SELECTED_EXERCISE] = event.exerciseId
            is WorkoutEvent.RepsChanged -> form.update { it.copy(reps = event.text) }
            is WorkoutEvent.WeightChanged -> form.update { it.copy(weight = event.text) }
            WorkoutEvent.AddSet -> addSet()
            is WorkoutEvent.DeleteSet -> viewModelScope.launch { workoutRepository.deleteSet(event.setId) }
        }
    }

    private fun addSet() {
        val exerciseId = uiState.value.selectedExerciseId ?: return
        val reps = (SetInput.parseReps(form.value.reps) as? Parsed.Valid)?.value ?: return
        val weight = (SetInput.parseWeightKg(form.value.weight) as? Parsed.Valid)?.value ?: return
        // Inputs are kept so the next (usually identical) set is one tap away.
        viewModelScope.launch { workoutRepository.addSet(exerciseId, reps, weight) }
    }

    private fun buildState(
        exercises: List<Exercise>,
        sets: List<WorkoutSet>,
        selectedId: String?,
        form: Form,
    ): WorkoutUiState {
        val reps = SetInput.parseReps(form.reps)
        val weight = SetInput.parseWeightKg(form.weight)
        val selected = exercises.find { it.id == selectedId } ?: exercises.firstOrNull()
        return WorkoutUiState(
            isLoading = false,
            exercises = exercises,
            selectedExerciseId = selected?.id,
            repsInput = form.reps,
            weightInput = form.weight,
            repsError = reps.visibleError(),
            weightError = weight.visibleError(),
            canAddSet = selected != null && reps is Parsed.Valid && weight is Parsed.Valid,
            days = groupByDay(sets),
        )
    }

    /** [sets] arrive newest first and groupBy keeps encounter order, so days stay newest first. */
    private fun groupByDay(sets: List<WorkoutSet>): List<DayGroup> {
        val zone = clock.zone
        return sets
            .map { it to Instant.ofEpochMilli(it.createdAt).atZone(zone) }
            .groupBy({ (_, dateTime) -> dateTime.toLocalDate() }) { (set, dateTime) ->
                LoggedSet(set.id, set.exerciseId, set.reps, set.weightKg, dateTime.toLocalTime())
            }
            .map { (date, daySets) -> DayGroup(date, daySets) }
    }

    /** An empty field disables "Add set" but is not shown as an error. */
    private fun Parsed<*>.visibleError(): InputError? =
        (this as? Parsed.Invalid)?.error?.takeUnless { it == InputError.Empty }

    companion object {
        const val KEY_SELECTED_EXERCISE = "selectedExerciseId"
    }
}
