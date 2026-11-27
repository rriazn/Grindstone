package com.example.grindstone.data

import com.example.grindstone.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.Clock
import javax.inject.Inject

interface ExerciseRepository {
    fun observeExercises(): Flow<List<Exercise>>
}

interface WorkoutRepository {
    /** All sets, newest first. */
    fun observeSets(): Flow<List<WorkoutSet>>

    suspend fun addSet(exerciseId: String, reps: Int, weightKg: Double)

    suspend fun deleteSet(id: String)
}

fun interface IdGenerator {
    fun newId(): String
}

class RoomExerciseRepository @Inject constructor(
    private val dao: ExerciseDao,
) : ExerciseRepository {
    override fun observeExercises(): Flow<List<Exercise>> = dao.observeAll()
}

class RoomWorkoutRepository @Inject constructor(
    private val dao: WorkoutSetDao,
    private val clock: Clock,
    private val idGenerator: IdGenerator,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : WorkoutRepository {
    override fun observeSets(): Flow<List<WorkoutSet>> = dao.observeAll()

    override suspend fun addSet(exerciseId: String, reps: Int, weightKg: Double) =
        withContext(ioDispatcher) {
            dao.insert(
                WorkoutSet(
                    id = idGenerator.newId(),
                    exerciseId = exerciseId,
                    reps = reps,
                    weightKg = weightKg,
                    createdAt = clock.millis(),
                ),
            )
        }

    override suspend fun deleteSet(id: String) = withContext(ioDispatcher) {
        dao.deleteById(id)
    }
}
