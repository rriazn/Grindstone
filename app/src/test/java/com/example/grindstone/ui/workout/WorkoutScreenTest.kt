package com.example.grindstone.ui.workout

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.grindstone.testing.FakeExerciseRepository
import com.example.grindstone.testing.FakeWorkoutRepository
import com.example.grindstone.testing.SEEDED_EXERCISES
import com.example.grindstone.ui.theme.GrindstoneTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

@RunWith(AndroidJUnit4::class)
// A tall screen keeps the whole LazyColumn composed, so list rows can be asserted directly.
@Config(qualifiers = "w411dp-h1600dp")
class WorkoutScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val loadedState = WorkoutUiState(
        isLoading = false,
        exercises = SEEDED_EXERCISES,
        selectedExerciseId = "bench_press",
    )

    private fun setScreen(state: WorkoutUiState, onEvent: (WorkoutEvent) -> Unit = {}) {
        composeRule.setContent {
            GrindstoneTheme(dynamicColor = false) {
                WorkoutScreen(state = state, onEvent = onEvent)
            }
        }
    }

    @Test
    fun showsTitleExercisesAndEmptyState() {
        setScreen(loadedState)

        composeRule.onNodeWithText("Grindstone").assertIsDisplayed()
        composeRule.onNodeWithText("Leg Press").assertIsDisplayed()
        composeRule.onNodeWithTag(WorkoutTestTags.exerciseChip("bench_press")).assertIsSelected()
        composeRule.onNodeWithTag(WorkoutTestTags.EMPTY_STATE).assertIsDisplayed()
    }

    @Test
    fun noEmptyStateWhileLoading() {
        setScreen(WorkoutUiState())

        composeRule.onNodeWithTag(WorkoutTestTags.EMPTY_STATE).assertDoesNotExist()
    }

    @Test
    fun addButtonFollowsValidity() {
        setScreen(loadedState.copy(canAddSet = false))
        composeRule.onNodeWithTag(WorkoutTestTags.ADD_SET_BUTTON).assertIsNotEnabled()
    }

    @Test
    fun addButtonIsEnabledForValidInputAndSendsEvent() {
        val events = mutableListOf<WorkoutEvent>()
        setScreen(loadedState.copy(repsInput = "5", weightInput = "60", canAddSet = true), events::add)

        composeRule.onNodeWithTag(WorkoutTestTags.ADD_SET_BUTTON).assertIsEnabled().performClick()

        assertEquals(listOf<WorkoutEvent>(WorkoutEvent.AddSet), events)
    }

    @Test
    fun showsInlineValidationMessages() {
        setScreen(loadedState.copy(repsError = InputError.OutOfRange, weightError = InputError.TooManyDecimals))

        composeRule.onNodeWithText("Reps must be from 1 to 999").assertIsDisplayed()
        composeRule.onNodeWithText("Use at most 2 decimal places").assertIsDisplayed()
    }

    @Test
    fun showsDayHeadersAndSetRowsAndSendsDelete() {
        val events = mutableListOf<WorkoutEvent>()
        val today = LocalDate.of(2026, 10, 7)
        setScreen(
            loadedState.copy(
                days = listOf(
                    DayGroup(today, listOf(LoggedSet("s2", "squat", 5, 102.5, LocalTime.of(18, 42)))),
                    DayGroup(today.minusDays(1), listOf(LoggedSet("s1", "dip", 10, 0.0, LocalTime.of(7, 5)))),
                ),
            ),
            events::add,
        )

        composeRule.onNodeWithTag(WorkoutTestTags.EMPTY_STATE).assertDoesNotExist()
        composeRule.onNodeWithTag(WorkoutTestTags.dayHeader(today)).assertIsDisplayed()
        composeRule.onNodeWithTag(WorkoutTestTags.dayHeader(today.minusDays(1))).assertIsDisplayed()
        composeRule.onNodeWithText("5 × 102.5 kg").assertIsDisplayed()
        composeRule.onNodeWithText("10 × 0 kg").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Delete set: Squat, 5 × 102.5 kg").performClick()

        assertEquals(listOf<WorkoutEvent>(WorkoutEvent.DeleteSet("s2")), events)
    }

    @Test
    @Config(qualifiers = "de-rDE-w411dp-h1600dp")
    fun weightUsesTheLocaleDecimalSeparator() {
        setScreen(
            loadedState.copy(
                days = listOf(
                    DayGroup(LocalDate.of(2026, 10, 7), listOf(LoggedSet("s", "squat", 5, 102.5, LocalTime.NOON))),
                ),
            ),
        )

        composeRule.onNodeWithText("5 × 102,5 kg").assertIsDisplayed()
    }

    @Test
    fun selectingAChipSendsEvent() {
        val events = mutableListOf<WorkoutEvent>()
        setScreen(loadedState, events::add)

        composeRule.onNodeWithTag(WorkoutTestTags.exerciseChip("deadlift")).performClick()

        assertEquals(listOf<WorkoutEvent>(WorkoutEvent.SelectExercise("deadlift")), events)
    }

    /** Whole screen wired to a real ViewModel (with fake repositories): type, add, see, delete. */
    @Test
    fun endToEndAddAndDeleteASet() {
        val workouts = FakeWorkoutRepository(nowMillis = Instant.parse("2026-10-07T18:00:00Z").toEpochMilli())
        val viewModel = WorkoutViewModel(
            FakeExerciseRepository(),
            workouts,
            Clock.fixed(Instant.parse("2026-10-07T18:00:00Z"), ZoneOffset.UTC),
            SavedStateHandle(),
        )
        composeRule.setContent {
            GrindstoneTheme(dynamicColor = false) { WorkoutRoute(viewModel) }
        }
        val addButton = composeRule.onNodeWithTag(WorkoutTestTags.ADD_SET_BUTTON)

        composeRule.onNodeWithTag(WorkoutTestTags.EMPTY_STATE).assertIsDisplayed()
        addButton.assertIsNotEnabled()

        composeRule.onNodeWithTag(WorkoutTestTags.exerciseChip("overhead_press")).performClick()
        composeRule.onNodeWithTag(WorkoutTestTags.REPS_INPUT).performTextInput("6")
        addButton.assertIsNotEnabled()
        composeRule.onNodeWithTag(WorkoutTestTags.WEIGHT_INPUT).performTextInput("42,5")
        addButton.assertIsEnabled().performClick()

        composeRule.onNodeWithTag(WorkoutTestTags.EMPTY_STATE).assertDoesNotExist()
        composeRule.onNodeWithTag(WorkoutTestTags.dayHeader(LocalDate.of(2026, 10, 7))).assertIsDisplayed()
        composeRule.onNodeWithTag(WorkoutTestTags.setRow("set-1")).assertIsDisplayed()
        composeRule.onNodeWithText("6 × 42.5 kg").assertIsDisplayed()

        composeRule.onNodeWithTag(WorkoutTestTags.deleteSet("set-1")).performClick()

        composeRule.onNodeWithTag(WorkoutTestTags.setRow("set-1")).assertDoesNotExist()
        composeRule.onNodeWithTag(WorkoutTestTags.EMPTY_STATE).assertIsDisplayed()
    }
}
