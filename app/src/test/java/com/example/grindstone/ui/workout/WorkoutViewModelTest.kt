package com.example.grindstone.ui.workout

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.example.grindstone.data.WorkoutSet
import com.example.grindstone.testing.FakeExerciseRepository
import com.example.grindstone.testing.FakeWorkoutRepository
import com.example.grindstone.testing.MainDispatcherRule
import com.example.grindstone.testing.SEEDED_EXERCISES
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val zone = ZoneId.of("Europe/Berlin")
    private val clock = Clock.fixed(Instant.parse("2026-10-07T16:00:00Z"), zone)
    private val exercises = FakeExerciseRepository()
    private val workouts = FakeWorkoutRepository(nowMillis = clock.millis())
    private val savedState = SavedStateHandle()

    private fun createViewModel() = WorkoutViewModel(exercises, workouts, clock, savedState)

    /** Keeps uiState subscribed (it uses WhileSubscribed) so `.value` stays current. */
    private fun TestScope.collecting(viewModel: WorkoutViewModel) = viewModel.also {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { it.uiState.collect() }
    }

    @Test
    fun startsLoadingThenShowsExercisesWithFirstSelectedAndEmptyLog() = runTest {
        val viewModel = createViewModel()
        assertTrue(viewModel.uiState.value.isLoading)

        viewModel.uiState.test {
            val loaded = expectMostRecentItem()
            assertFalse(loaded.isLoading)
            assertEquals(SEEDED_EXERCISES, loaded.exercises)
            assertEquals("bench_press", loaded.selectedExerciseId)
            assertEquals(emptyList<DayGroup>(), loaded.days)
            assertFalse(loaded.canAddSet)
        }
    }

    @Test
    fun eachEventEmitsANewState() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            expectMostRecentItem()
            viewModel.onEvent(WorkoutEvent.RepsChanged("5"))
            assertEquals("5", awaitItem().repsInput)
            viewModel.onEvent(WorkoutEvent.WeightChanged("50"))
            assertTrue(awaitItem().canAddSet)
            viewModel.onEvent(WorkoutEvent.AddSet)
            assertEquals(1, awaitItem().days.size)
        }
    }

    @Test
    fun selectingAnExerciseUpdatesSelection() = runTest {
        val viewModel = collecting(createViewModel())

        viewModel.onEvent(WorkoutEvent.SelectExercise("deadlift"))

        assertEquals("deadlift", viewModel.uiState.value.selectedExerciseId)
    }

    @Test
    fun selectionSurvivesViewModelRecreationViaSavedState() = runTest {
        createViewModel().onEvent(WorkoutEvent.SelectExercise("dip"))

        val recreated = collecting(createViewModel())

        assertEquals("dip", recreated.uiState.value.selectedExerciseId)
    }

    @Test
    fun unknownSavedSelectionFallsBackToFirstExercise() = runTest {
        savedState[WorkoutViewModel.KEY_SELECTED_EXERCISE] = "removed_exercise"

        val viewModel = collecting(createViewModel())

        assertEquals("bench_press", viewModel.uiState.value.selectedExerciseId)
    }

    @Test
    fun emptyInputsDisableAddWithoutShowingErrors() = runTest {
        val viewModel = collecting(createViewModel())

        viewModel.onEvent(WorkoutEvent.RepsChanged(""))
        viewModel.onEvent(WorkoutEvent.WeightChanged(""))

        with(viewModel.uiState.value) {
            assertNull(repsError)
            assertNull(weightError)
            assertFalse(canAddSet)
        }
    }

    @Test
    fun invalidInputsShowErrorsAndDisableAdd() = runTest {
        val viewModel = collecting(createViewModel())

        viewModel.onEvent(WorkoutEvent.RepsChanged("0"))
        viewModel.onEvent(WorkoutEvent.WeightChanged("abc"))

        with(viewModel.uiState.value) {
            assertEquals("0", repsInput)
            assertEquals("abc", weightInput)
            assertEquals(InputError.OutOfRange, repsError)
            assertEquals(InputError.NotANumber, weightError)
            assertFalse(canAddSet)
        }
    }

    @Test
    fun oneValidFieldIsNotEnoughToAdd() = runTest {
        val viewModel = collecting(createViewModel())

        viewModel.onEvent(WorkoutEvent.RepsChanged("5"))

        assertNull(viewModel.uiState.value.repsError)
        assertFalse(viewModel.uiState.value.canAddSet)
    }

    @Test
    fun fixingInputClearsErrorAndEnablesAdd() = runTest {
        val viewModel = collecting(createViewModel())
        viewModel.onEvent(WorkoutEvent.RepsChanged("5"))
        viewModel.onEvent(WorkoutEvent.WeightChanged("62,555"))
        assertEquals(InputError.TooManyDecimals, viewModel.uiState.value.weightError)

        viewModel.onEvent(WorkoutEvent.WeightChanged("62,5"))

        assertNull(viewModel.uiState.value.weightError)
        assertTrue(viewModel.uiState.value.canAddSet)
    }

    @Test
    fun addSetStoresSelectedExerciseAndParsedValuesAndKeepsInputs() = runTest {
        val viewModel = collecting(createViewModel())
        viewModel.onEvent(WorkoutEvent.SelectExercise("squat"))
        viewModel.onEvent(WorkoutEvent.RepsChanged("5"))
        viewModel.onEvent(WorkoutEvent.WeightChanged("102,5"))

        viewModel.onEvent(WorkoutEvent.AddSet)

        assertEquals(
            listOf(WorkoutSet("set-1", "squat", 5, 102.5, clock.millis())),
            workouts.sets.value,
        )
        with(viewModel.uiState.value) {
            assertEquals(
                listOf(DayGroup(LocalDate.of(2026, 10, 7), listOf(LoggedSet("set-1", "squat", 5, 102.5, LocalTime.of(18, 0))))),
                days,
            )
            assertEquals("5", repsInput)
            assertEquals("102,5", weightInput)
        }
    }

    @Test
    fun addSetWithInvalidInputDoesNothing() = runTest {
        val viewModel = collecting(createViewModel())
        viewModel.onEvent(WorkoutEvent.RepsChanged("5"))
        viewModel.onEvent(WorkoutEvent.WeightChanged("1001"))

        viewModel.onEvent(WorkoutEvent.AddSet)

        assertEquals(emptyList<WorkoutSet>(), workouts.sets.value)
    }

    @Test
    fun deleteSetRemovesIt() = runTest {
        workouts.sets.value = listOf(set("a", "2026-10-07T10:00:00Z"), set("b", "2026-10-07T11:00:00Z"))
        val viewModel = collecting(createViewModel())

        viewModel.onEvent(WorkoutEvent.DeleteSet("b"))

        assertEquals(listOf("a"), viewModel.uiState.value.days.flatMap { day -> day.sets.map { it.id } })
    }

    @Test
    fun setsAreGroupedByLocalDayNewestFirst() = runTest {
        workouts.sets.value = listOf(
            set("mon-morning", "2026-10-05T06:00:00Z"),
            set("mon-evening", "2026-10-05T18:00:00Z"),
            // 22:30 UTC is already Wednesday 00:30 in Berlin.
            set("wed-midnight", "2026-10-06T22:30:00Z"),
            set("tue", "2026-10-06T12:00:00Z"),
        )

        val viewModel = collecting(createViewModel())

        assertEquals(
            listOf(
                LocalDate.of(2026, 10, 7) to listOf("wed-midnight"),
                LocalDate.of(2026, 10, 6) to listOf("tue"),
                LocalDate.of(2026, 10, 5) to listOf("mon-evening", "mon-morning"),
            ),
            viewModel.uiState.value.days.map { day -> day.date to day.sets.map { it.id } },
        )
        assertEquals(LocalTime.of(0, 30), viewModel.uiState.value.days.first().sets.single().time)
    }

    @Test
    fun loggedSetsSurviveViewModelRecreation() = runTest {
        val first = collecting(createViewModel())
        first.onEvent(WorkoutEvent.RepsChanged("8"))
        first.onEvent(WorkoutEvent.WeightChanged("60"))
        first.onEvent(WorkoutEvent.AddSet)

        val recreated = collecting(createViewModel())

        assertEquals(listOf("set-1"), recreated.uiState.value.days.flatMap { day -> day.sets.map { it.id } })
    }

    private fun set(id: String, instant: String) =
        WorkoutSet(id, "bench_press", reps = 8, weightKg = 60.0, createdAt = Instant.parse(instant).toEpochMilli())
}
