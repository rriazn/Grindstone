package com.example.grindstone.ui.workout

import com.example.grindstone.data.SEEDED_EXERCISE_IDS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ExerciseNamesTest {
    @Test
    fun everySeededExerciseHasItsOwnDisplayName() {
        val names = SEEDED_EXERCISE_IDS.map { id -> exerciseNameRes(id).also { assertNotNull(id, it) } }

        assertEquals(SEEDED_EXERCISE_IDS.size, names.toSet().size)
    }
}
