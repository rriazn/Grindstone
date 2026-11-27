package com.example.grindstone.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@RunWith(AndroidJUnit4::class)
class RepositoriesTest {
    private val db = GrindstoneDatabase.create(
        ApplicationProvider.getApplicationContext<Context>(),
        name = null,
    )
    private val now = Instant.parse("2026-10-07T18:30:00Z")
    private var nextId = 0

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun exerciseRepositoryExposesSeededExercises() = runTest {
        val exercises = RoomExerciseRepository(db.exerciseDao()).observeExercises().first()

        assertEquals(SEEDED_EXERCISE_IDS, exercises.map { it.id })
    }

    @Test
    fun addSetStoresGeneratedIdAndCurrentTimeInUtcMillis() = runTest {
        val repository = workoutRepository(StandardTestDispatcher(testScheduler))

        repository.addSet("deadlift", reps = 3, weightKg = 140.5)

        assertEquals(
            listOf(WorkoutSet("id-1", "deadlift", 3, 140.5, now.toEpochMilli())),
            repository.observeSets().first(),
        )
    }

    @Test
    fun observeSetsEmitsOnAddAndDelete() = runTest {
        val repository = workoutRepository(StandardTestDispatcher(testScheduler))

        repository.observeSets().test {
            assertEquals(emptyList<WorkoutSet>(), awaitItem())

            repository.addSet("squat", reps = 5, weightKg = 100.0)
            assertEquals(listOf("id-1"), awaitItem().map { it.id })

            repository.deleteSet("id-1")
            assertEquals(emptyList<WorkoutSet>(), awaitItem())
        }
    }

    private fun workoutRepository(dispatcher: TestDispatcher) = RoomWorkoutRepository(
        dao = db.workoutSetDao(),
        clock = Clock.fixed(now, ZoneOffset.UTC),
        idGenerator = { "id-${++nextId}" },
        ioDispatcher = dispatcher,
    )
}
