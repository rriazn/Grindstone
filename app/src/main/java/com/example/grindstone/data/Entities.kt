package com.example.grindstone.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A predefined exercise. [id] is a stable key (e.g. "bench_press"); the display name
 * comes from string resources so it can be localized.
 */
@Entity(tableName = "exercises")
data class Exercise(
    @PrimaryKey val id: String,
    val position: Int,
)

/**
 * One logged set. [id] is a client-generated UUID and [createdAt] is epoch millis (UTC),
 * so records can later be synced without id collisions or time zone ambiguity.
 */
@Entity(
    tableName = "workout_sets",
    foreignKeys = [
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
        ),
    ],
    indices = [Index("exerciseId"), Index("createdAt")],
)
data class WorkoutSet(
    @PrimaryKey val id: String,
    val exerciseId: String,
    val reps: Int,
    val weightKg: Double,
    val createdAt: Long,
)
