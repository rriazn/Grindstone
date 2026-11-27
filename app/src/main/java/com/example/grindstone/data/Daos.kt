package com.example.grindstone.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises ORDER BY position")
    fun observeAll(): Flow<List<Exercise>>
}

@Dao
interface WorkoutSetDao {
    /** Newest first; id breaks ties so the order is stable. */
    @Query("SELECT * FROM workout_sets ORDER BY createdAt DESC, id DESC")
    fun observeAll(): Flow<List<WorkoutSet>>

    @Insert
    suspend fun insert(set: WorkoutSet)

    @Query("DELETE FROM workout_sets WHERE id = :id")
    suspend fun deleteById(id: String)
}
