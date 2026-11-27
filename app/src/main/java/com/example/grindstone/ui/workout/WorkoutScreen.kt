package com.example.grindstone.ui.workout

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.grindstone.R
import com.example.grindstone.data.Exercise
import com.example.grindstone.data.SEEDED_EXERCISE_IDS
import com.example.grindstone.ui.theme.GrindstoneTheme
import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

object WorkoutTestTags {
    const val SET_LIST = "set_list"
    const val REPS_INPUT = "reps_input"
    const val WEIGHT_INPUT = "weight_input"
    const val ADD_SET_BUTTON = "add_set_button"
    const val EMPTY_STATE = "empty_state"
    fun exerciseChip(exerciseId: String) = "exercise_chip_$exerciseId"
    fun dayHeader(date: LocalDate) = "day_header_$date"
    fun setRow(setId: String) = "set_row_$setId"
    fun deleteSet(setId: String) = "delete_set_$setId"
}

/** Stateful entry point: wires the Hilt ViewModel to the stateless [WorkoutScreen]. */
@Composable
fun WorkoutRoute(viewModel: WorkoutViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    WorkoutScreen(state = state, onEvent = viewModel::onEvent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(
    state: WorkoutUiState,
    onEvent: (WorkoutEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
                .testTag(WorkoutTestTags.SET_LIST),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "picker") {
                ExercisePicker(state.exercises, state.selectedExerciseId, onEvent)
            }
            item(key = "inputs") {
                SetInputs(state, onEvent)
            }
            item(key = "add") {
                Button(
                    onClick = { onEvent(WorkoutEvent.AddSet) },
                    enabled = state.canAddSet,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag(WorkoutTestTags.ADD_SET_BUTTON),
                ) {
                    Text(stringResource(R.string.add_set))
                }
            }
            if (!state.isLoading && state.days.isEmpty()) {
                item(key = "empty") {
                    Text(
                        text = stringResource(R.string.empty_state),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(vertical = 32.dp)
                            .testTag(WorkoutTestTags.EMPTY_STATE),
                    )
                }
            }
            state.days.forEach { day ->
                item(key = "day_${day.date}") {
                    DayHeader(day.date)
                }
                items(day.sets, key = { it.id }) { set ->
                    SetRow(set, onDelete = { onEvent(WorkoutEvent.DeleteSet(set.id)) })
                }
            }
        }
    }
}

@Composable
private fun ExercisePicker(
    exercises: List<Exercise>,
    selectedExerciseId: String?,
    onEvent: (WorkoutEvent) -> Unit,
) {
    Column {
        Text(
            text = stringResource(R.string.exercise_picker_label),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.semantics { heading() },
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            exercises.forEach { exercise ->
                FilterChip(
                    selected = exercise.id == selectedExerciseId,
                    onClick = { onEvent(WorkoutEvent.SelectExercise(exercise.id)) },
                    label = { Text(exerciseName(exercise.id)) },
                    modifier = Modifier.testTag(WorkoutTestTags.exerciseChip(exercise.id)),
                )
            }
        }
    }
}

@Composable
private fun SetInputs(state: WorkoutUiState, onEvent: (WorkoutEvent) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = state.repsInput,
            onValueChange = { onEvent(WorkoutEvent.RepsChanged(it)) },
            label = { Text(stringResource(R.string.reps_label)) },
            isError = state.repsError != null,
            supportingText = state.repsError?.let { error -> { Text(repsErrorText(error)) } },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next,
            ),
            modifier = Modifier
                .weight(1f)
                .testTag(WorkoutTestTags.REPS_INPUT),
        )
        OutlinedTextField(
            value = state.weightInput,
            onValueChange = { onEvent(WorkoutEvent.WeightChanged(it)) },
            label = { Text(stringResource(R.string.weight_label)) },
            isError = state.weightError != null,
            supportingText = state.weightError?.let { error -> { Text(weightErrorText(error)) } },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onDone = { if (state.canAddSet) onEvent(WorkoutEvent.AddSet) },
            ),
            modifier = Modifier
                .weight(1f)
                .testTag(WorkoutTestTags.WEIGHT_INPUT),
        )
    }
}

@Composable
private fun repsErrorText(error: InputError): String = when (error) {
    InputError.OutOfRange ->
        stringResource(R.string.reps_error_out_of_range, SetInput.MIN_REPS, SetInput.MAX_REPS)
    InputError.Empty, InputError.NotANumber, InputError.TooManyDecimals ->
        stringResource(R.string.reps_error_not_a_number)
}

@Composable
private fun weightErrorText(error: InputError): String = when (error) {
    InputError.OutOfRange -> stringResource(
        R.string.weight_error_out_of_range,
        SetInput.MIN_WEIGHT_KG,
        SetInput.MAX_WEIGHT_KG,
    )
    InputError.TooManyDecimals -> pluralStringResource(
        R.plurals.weight_error_too_many_decimals,
        SetInput.MAX_WEIGHT_DECIMALS,
        SetInput.MAX_WEIGHT_DECIMALS,
    )
    InputError.Empty, InputError.NotANumber -> stringResource(R.string.weight_error_not_a_number)
}

@Composable
private fun DayHeader(date: LocalDate) {
    val formatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL) }
    Column(modifier = Modifier.padding(top = 16.dp)) {
        Text(
            text = date.format(formatter),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .semantics { heading() }
                .testTag(WorkoutTestTags.dayHeader(date)),
        )
        HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun SetRow(set: LoggedSet, onDelete: () -> Unit) {
    val weightFormat = remember {
        NumberFormat.getNumberInstance().apply { maximumFractionDigits = SetInput.MAX_WEIGHT_DECIMALS }
    }
    val timeFormatter = remember { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT) }
    val name = exerciseName(set.exerciseId)
    val summary = stringResource(R.string.set_summary, set.reps, weightFormat.format(set.weightKg))
    ListItem(
        headlineContent = { Text(name) },
        supportingContent = { Text(summary) },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(set.time.format(timeFormatter), style = MaterialTheme.typography.labelMedium)
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag(WorkoutTestTags.deleteSet(set.id)),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_delete),
                        contentDescription = stringResource(R.string.delete_set_description, name, summary),
                    )
                }
            }
        },
        modifier = Modifier.testTag(WorkoutTestTags.setRow(set.id)),
    )
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WorkoutScreenPreview() {
    val today = LocalDate.of(2026, 10, 7)
    GrindstoneTheme(dynamicColor = false) {
        WorkoutScreen(
            state = WorkoutUiState(
                isLoading = false,
                exercises = SEEDED_EXERCISE_IDS.mapIndexed { i, id -> Exercise(id, i) },
                selectedExerciseId = "squat",
                repsInput = "5",
                weightInput = "102,5",
                canAddSet = true,
                days = listOf(
                    DayGroup(
                        today,
                        listOf(
                            LoggedSet("2", "squat", 5, 102.5, LocalTime.of(18, 42)),
                            LoggedSet("1", "squat", 5, 100.0, LocalTime.of(18, 37)),
                        ),
                    ),
                    DayGroup(
                        today.minusDays(2),
                        listOf(LoggedSet("0", "pull_up", 8, 0.0, LocalTime.of(7, 5))),
                    ),
                ),
            ),
            onEvent = {},
        )
    }
}
