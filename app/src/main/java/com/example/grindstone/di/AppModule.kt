package com.example.grindstone.di

import android.content.Context
import com.example.grindstone.data.ExerciseDao
import com.example.grindstone.data.ExerciseRepository
import com.example.grindstone.data.GrindstoneDatabase
import com.example.grindstone.data.IdGenerator
import com.example.grindstone.data.RoomExerciseRepository
import com.example.grindstone.data.RoomWorkoutRepository
import com.example.grindstone.data.WorkoutRepository
import com.example.grindstone.data.WorkoutSetDao
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import java.time.Clock
import java.util.UUID
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): GrindstoneDatabase =
        GrindstoneDatabase.create(context)

    @Provides
    fun provideExerciseDao(db: GrindstoneDatabase): ExerciseDao = db.exerciseDao()

    @Provides
    fun provideWorkoutSetDao(db: GrindstoneDatabase): WorkoutSetDao = db.workoutSetDao()

    /** Not a singleton, so a time zone change is picked up by the next screen. */
    @Provides
    fun provideClock(): Clock = Clock.systemDefaultZone()

    @Provides
    fun provideIdGenerator(): IdGenerator = IdGenerator { UUID.randomUUID().toString() }

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindExerciseRepository(impl: RoomExerciseRepository): ExerciseRepository

    @Binds
    abstract fun bindWorkoutRepository(impl: RoomWorkoutRepository): WorkoutRepository
}
