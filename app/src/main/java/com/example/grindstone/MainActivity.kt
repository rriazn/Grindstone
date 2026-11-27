package com.example.grindstone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.grindstone.ui.theme.GrindstoneTheme
import com.example.grindstone.ui.workout.WorkoutRoute
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GrindstoneTheme {
                WorkoutRoute()
            }
        }
    }
}
