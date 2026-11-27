package com.example.grindstone.ui.workout

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.grindstone.R

/** Maps a seeded exercise key to its localizable display name. */
@StringRes
fun exerciseNameRes(exerciseId: String): Int? = when (exerciseId) {
    "bench_press" -> R.string.exercise_bench_press
    "squat" -> R.string.exercise_squat
    "deadlift" -> R.string.exercise_deadlift
    "overhead_press" -> R.string.exercise_overhead_press
    "barbell_row" -> R.string.exercise_barbell_row
    "pull_up" -> R.string.exercise_pull_up
    "dip" -> R.string.exercise_dip
    "bicep_curl" -> R.string.exercise_bicep_curl
    "lat_pulldown" -> R.string.exercise_lat_pulldown
    "leg_press" -> R.string.exercise_leg_press
    else -> null
}

/** Falls back to the raw key, which only happens if a seeded id lacks a string. */
@Composable
fun exerciseName(exerciseId: String): String =
    exerciseNameRes(exerciseId)?.let { stringResource(it) } ?: exerciseId
