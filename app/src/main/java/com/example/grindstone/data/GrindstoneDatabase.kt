package com.example.grindstone.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Stable keys of the exercises seeded on database creation, in display order. */
val SEEDED_EXERCISE_IDS = listOf(
    "bench_press",
    "squat",
    "deadlift",
    "overhead_press",
    "barbell_row",
    "pull_up",
    "dip",
    "bicep_curl",
    "lat_pulldown",
    "leg_press",
)

@Database(
    entities = [Exercise::class, WorkoutSet::class],
    version = GrindstoneDatabase.VERSION,
    exportSchema = true,
)
abstract class GrindstoneDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutSetDao(): WorkoutSetDao

    companion object {
        const val VERSION = 1
        const val NAME = "grindstone.db"

        /**
         * Migration strategy: every schema change bumps [VERSION], commits the exported
         * schema in app/schemas and adds an explicit [Migration] here. There is deliberately
         * no destructive fallback, so a missing migration fails loudly instead of wiping data.
         */
        val MIGRATIONS: Array<Migration> = emptyArray()

        /** Builds the database; pass `name = null` for an in-memory database (tests). */
        fun create(context: Context, name: String? = NAME): GrindstoneDatabase {
            val builder = if (name == null) {
                Room.inMemoryDatabaseBuilder(context, GrindstoneDatabase::class.java)
            } else {
                Room.databaseBuilder(context, GrindstoneDatabase::class.java, name)
            }
            return builder
                .addMigrations(*MIGRATIONS)
                .addCallback(SeedCallback)
                .build()
        }
    }

    private object SeedCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            SEEDED_EXERCISE_IDS.forEachIndexed { position, id ->
                db.execSQL(
                    "INSERT INTO exercises (id, position) VALUES (?, ?)",
                    arrayOf<Any>(id, position),
                )
            }
        }
    }
}
