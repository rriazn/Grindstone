package com.example.grindstone.testing

import com.example.grindstone.data.Exercise
import com.example.grindstone.data.ExerciseRepository
import com.example.grindstone.data.SEEDED_EXERCISE_IDS
import com.example.grindstone.data.WorkoutRepository
import com.example.grindstone.data.WorkoutSet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

val SEEDED_EXERCISES = SEEDED_EXERCISE_IDS.mapIndexed { position, id -> Exercise(id, position) }

class FakeExerciseRepository(exercises: List<Exercise> = SEEDED_EXERCISES) : ExerciseRepository {
    val exercises = MutableStateFlow(exercises)
    override fun observeExercises(): Flow<List<Exercise>> = exercises
}

/** In-memory [WorkoutRepository] with the same ordering contract as the Room implementation. */
class FakeWorkoutRepository(var nowMillis: Long = 0) : WorkoutRepository {
    val sets = MutableStateFlow<List<WorkoutSet>>(emptyList())
    private var nextId = 0

    override fun observeSets(): Flow<List<WorkoutSet>> = sets.map { all ->
        all.sortedWith(compareByDescending<WorkoutSet> { it.createdAt }.thenByDescending { it.id })
    }

    override suspend fun addSet(exerciseId: String, reps: Int, weightKg: Double) {
        sets.update { it + WorkoutSet("set-${++nextId}", exerciseId, reps, weightKg, nowMillis) }
    }

    override suspend fun deleteSet(id: String) {
        sets.update { all -> all.filterNot { it.id == id } }
    }
}
