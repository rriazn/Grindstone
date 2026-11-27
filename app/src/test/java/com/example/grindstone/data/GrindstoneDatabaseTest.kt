package com.example.grindstone.data

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GrindstoneDatabaseTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var db: GrindstoneDatabase

    @Before
    fun setUp() {
        db = GrindstoneDatabase.create(context, name = null)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun seededExercisesExistInDisplayOrder() = runTest {
        val exercises = db.exerciseDao().observeAll().first()

        assertEquals(SEEDED_EXERCISE_IDS, exercises.map { it.id })
        assertEquals(10, exercises.size)
    }

    @Test
    fun insertedSetsAreReturnedNewestFirst() = runTest {
        val dao = db.workoutSetDao()
        dao.insert(set(id = "a", createdAt = 1_000))
        dao.insert(set(id = "c", createdAt = 3_000))
        dao.insert(set(id = "b", createdAt = 2_000))

        assertEquals(listOf("c", "b", "a"), dao.observeAll().first().map { it.id })
    }

    @Test
    fun setsWithEqualTimestampHaveStableOrder() = runTest {
        val dao = db.workoutSetDao()
        dao.insert(set(id = "a", createdAt = 1_000))
        dao.insert(set(id = "b", createdAt = 1_000))

        assertEquals(listOf("b", "a"), dao.observeAll().first().map { it.id })
    }

    @Test
    fun insertedSetKeepsAllFields() = runTest {
        val set = WorkoutSet("id-1", "squat", reps = 5, weightKg = 102.5, createdAt = 42)
        db.workoutSetDao().insert(set)

        assertEquals(listOf(set), db.workoutSetDao().observeAll().first())
    }

    @Test
    fun deleteRemovesOnlyThatSet() = runTest {
        val dao = db.workoutSetDao()
        dao.insert(set(id = "keep", createdAt = 1))
        dao.insert(set(id = "drop", createdAt = 2))

        dao.deleteById("drop")

        assertEquals(listOf("keep"), dao.observeAll().first().map { it.id })
    }

    @Test
    fun setForUnknownExerciseIsRejected() = runTest {
        val result = runCatching { db.workoutSetDao().insert(set(id = "x", exerciseId = "unknown")) }

        assertTrue(result.exceptionOrNull() is SQLiteConstraintException)
    }

    @Test
    fun dataPersistsAcrossReopeningTheDatabase() = runTest {
        val name = "persistence-test.db"
        context.deleteDatabase(name)
        val first = GrindstoneDatabase.create(context, name)
        first.workoutSetDao().insert(set(id = "persisted", createdAt = 7))
        first.close()

        val reopened = GrindstoneDatabase.create(context, name)
        try {
            assertEquals(listOf("persisted"), reopened.workoutSetDao().observeAll().first().map { it.id })
            // Seeding runs only on creation, so reopening must not duplicate exercises.
            assertEquals(SEEDED_EXERCISE_IDS, reopened.exerciseDao().observeAll().first().map { it.id })
        } finally {
            reopened.close()
            context.deleteDatabase(name)
        }
    }

    private fun set(id: String, createdAt: Long = 0, exerciseId: String = "bench_press") =
        WorkoutSet(id = id, exerciseId = exerciseId, reps = 8, weightKg = 60.0, createdAt = createdAt)
}
